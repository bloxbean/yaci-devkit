#!/usr/bin/env node
import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Minimal arg parsing
const args = process.argv.slice(2);
const getArg = (name, def) => {
  const idx = args.indexOf(`--${name}`);
  if (idx >= 0 && args[idx + 1]) return args[idx + 1];
  return def;
};

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Resolve server URL
const indexerBase = process.env.PUBLIC_INDEXER_BASE_URL || 'http://localhost:8080/api/v1';
const fallbackServer = (() => {
  try { return new URL(indexerBase).origin; } catch { return 'http://localhost:8080'; }
})();
const server = getArg('server', process.env.INDEXER_SERVER || fallbackServer);
const openApiUrl = `${server.replace(/\/$/, '')}/v3/api-docs`;

// Resolve output path
const outArg = getArg('out');
const repoRoot = path.resolve(__dirname, '../../..');
const defaultOut = path.resolve(repoRoot, 'adr/0002-api-catalog.md');
const outFile = outArg ? path.resolve(process.cwd(), outArg) : defaultOut;

const isHttpOk = (res) => res && (res.status === 200);

function mdEscape(str = '') {
  return String(str);
}

function buildCatalog(doc) {
  const lines = [];
  lines.push('# API Catalog');
  lines.push('');
  lines.push('Generated from /v3/api-docs');
  lines.push('');
  const title = doc?.info?.title || '';
  const version = doc?.info?.version || '';
  lines.push(`- Title: ${title} (v${version})`);
  if (Array.isArray(doc?.servers) && doc.servers[0]?.url) {
    lines.push(`- Server: ${doc.servers[0].url}`);
  }
  lines.push('');

  const paths = doc?.paths || {};
  const methods = ['get','post','put','patch','delete','head','options'];
  for (const [p, def] of Object.entries(paths)) {
    for (const m of methods) {
      const op = def?.[m];
      if (!op) continue;
      lines.push(`## ${m.toUpperCase()} \`${p}\``);
      const summary = op.summary || op.operationId || '';
      if (summary) lines.push(`- Summary: ${mdEscape(summary)}`);
      if (Array.isArray(op.tags) && op.tags.length) {
        lines.push(`- Tags: ${op.tags.join(', ')}`);
      }
      if (Array.isArray(op.parameters) && op.parameters.length) {
        lines.push('- Params:');
        for (const prm of op.parameters) {
          const req = prm.required ? ' [required]' : '';
          const typ = prm?.schema?.type ? `: ${prm.schema.type}` : '';
          lines.push(`  - \`${prm.name}\` (${prm.in})${req}${typ}`);
        }
      }
      const content = op?.requestBody?.content;
      if (content && typeof content === 'object') {
        lines.push(`- Request: ${Object.keys(content).join(', ')}`);
      }
      const responses = op?.responses || {};
      for (const [code, r] of Object.entries(responses)) {
        lines.push(`- Response ${code}: ${r?.description || ''}`);
      }
    }
  }

  return lines.join('\n');
}

async function main() {
  try {
    const res = await fetch(openApiUrl);
    if (!isHttpOk(res)) {
      throw new Error(`Failed to fetch OpenAPI: ${res.status} ${res.statusText}`);
    }
    const doc = await res.json();
    const md = buildCatalog(doc);
    await fs.mkdir(path.dirname(outFile), { recursive: true });
    await fs.writeFile(outFile, md, 'utf8');
    console.log(`API catalog written to: ${outFile}`);
  } catch (e) {
    console.error(`Error generating API catalog from ${openApiUrl}:`, e.message || e);
    process.exit(1);
  }
}

main();

