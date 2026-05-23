Stomp client fallback

This folder is used as a local fallback for the STOMP UMD bundle. If CDN delivery fails or a user is in an environment that blocks CDN scripts, copy a compatible STOMP UMD bundle here as `stomp.umd.min.js`.

Recommended source:
https://unpkg.com/@stomp/stompjs@6.1.2/bundles/stomp.umd.min.js

Example (from project root):

  curl -L -o src/main/resources/static/js/stomp.umd.min.js \
    "https://unpkg.com/@stomp/stompjs@6.1.2/bundles/stomp.umd.min.js"

Note: Licensing for third-party libraries applies — ensure the file you add is compatible with your distribution policies.
