#!/usr/bin/env node

import path from 'path';
import { fileURLToPath } from 'url';

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
