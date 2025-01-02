#!/usr/bin/env node

import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';
import yargs from 'yargs';
import { hideBin } from 'yargs/helpers';

dotenv.config();

// Parse command-line arguments
const argv = yargs(hideBin(process.argv))
    .option('indexerBaseUrl', {
        alias: 'b',
        type: 'string',
        description: 'The base URL for the indexer API',
        default: process.env.PUBLIC_INDEXER_BASE_URL || 'http://localhost:8080/api/v1'
    })
    .option('indexerWsUrl', {
        alias: 'w',
        type: 'string',
        description: 'The WebSocket URL for live updates',
        default: process.env.PUBLIC_INDEXER_WS_URL || 'ws://localhost:8080/ws/liveblocks'
    })
    .argv;

// Set environment variables
process.env.PUBLIC_INDEXER_BASE_URL = argv.indexerBaseUrl;
process.env.PUBLIC_INDEXER_WS_URL = argv.indexerWsUrl;

// Resolve the correct path to build/index.js
const __dirname = path.dirname(fileURLToPath(import.meta.url));
const appPath = path.resolve(__dirname, 'build', 'index.js');

// Dynamically import the build/index.js file
import(appPath)
    .then(() => {
        console.log('Yaci Viewer started successfully!');
    })
    .catch((err) => {
        console.error('Error starting Yaci Viewer:', err);
        process.exit(1);
    });
