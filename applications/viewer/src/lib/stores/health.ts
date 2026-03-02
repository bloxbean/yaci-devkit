import { writable } from 'svelte/store';
import { env } from '$env/dynamic/public';

export type HealthStatus = 'unknown' | 'ok' | 'down';

export interface HealthState {
  status: HealthStatus;
  lastChecked?: number;
  latencyMs?: number;
  error?: string;
}

export const health = writable<HealthState>({ status: 'unknown' });

let timer: any = null;
let inFlight = false;

export async function checkHealth(): Promise<void> {
  if (inFlight) return; // dedupe
  inFlight = true;
  const started = Date.now();
  try {
    const base = env.PUBLIC_INDEXER_CLIENT_BASE_URL || env.PUBLIC_INDEXER_BASE_URL;
    if (!base) throw new Error('Missing PUBLIC_INDEXER_BASE_URL');
    const res = await fetch(`${base}/network`, { method: 'GET' });
    const latency = Date.now() - started;
    if (res.ok) {
      health.set({ status: 'ok', lastChecked: Date.now(), latencyMs: latency });
    } else {
      health.set({ status: 'down', lastChecked: Date.now(), latencyMs: latency, error: `${res.status} ${res.statusText}` });
    }
  } catch (e: any) {
    const latency = Date.now() - started;
    health.set({ status: 'down', lastChecked: Date.now(), latencyMs: latency, error: e?.message || 'Network error' });
  } finally {
    inFlight = false;
  }
}

export function startHealthPolling(intervalMs = 15000) {
  stopHealthPolling();
  const tick = async () => {
    await checkHealth();
    timer = setTimeout(tick, intervalMs);
  };
  // kick off quickly
  tick();
}

export function stopHealthPolling() {
  if (timer) {
    clearTimeout(timer);
    timer = null;
  }
}

