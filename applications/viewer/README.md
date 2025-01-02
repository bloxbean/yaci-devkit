## Getting Started

### Installation

To install Yaci Viewer globally, run:

```bash
npm install -g @bloxbean/yaci-viewer
```

### Running Yaci Viewer

1. Start the Yaci DevKit.
2. Run the following command:

```bash
yaci-viewer
```

By default, Yaci Viewer connects to a Yaci-Store backend running on `localhost` at port `8080`.

---

## Configuration Options

If you need to connect to a Yaci-Store backend running on a different host or port, you can configure Yaci Viewer in one of the following ways:

### Option 1: Using a `.env` File

Create a `.env` file in the same directory where you are starting Yaci Viewer, and specify the following environment variables:

- `PUBLIC_INDEXER_BASE_URL`: The base URL for the indexer API. Default: `http://localhost:8080/api/v1`.
- `PUBLIC_INDEXER_WS_URL`: The WebSocket URL for live updates. Default: `ws://localhost:8080/ws/liveblocks`.

### Option 2: Using Command-Line Arguments

You can also specify the indexer base URL and WebSocket URL directly via command-line arguments:

```bash
yaci-viewer --indexerBaseUrl http://new-url.com/api/v1 --indexerWsUrl ws://new-url.com/ws/liveblocks
```

---

## Notes

- Ensure that the YaciDevKit or Yaci-Store backend is running and accessible before starting Yaci Viewer.

## Developing

Once you've checked out the project and installed dependencies with `npm install`  start a development server:

```bash
npm run dev

# or start the server and open the app in a new browser tab
npm run dev -- --open
```

> **Note:** Ensure that Yaci DevKit is running and that you have created a `.env` file with the indexer URLs in the viewer folder. For the required environment variables, refer to the `env.example` file.

## Building

To create a production version of the app:

```bash
npm run build
```

