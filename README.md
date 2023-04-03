## Developing

Once you've checked out the project and installed dependencies with `npm install`  start a development server:

```bash
npm run dev

# or start the server and open the app in a new browser tab
npm run dev -- --open
```

## Building

To create a production version of your app:

```bash
npm run build
```

## Docker Build

```shell
docker buildx build --push --platform linux/amd64,linux/arm64 --tag bloxbean/yaci-viewer:<version> . 
```
