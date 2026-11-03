#!/usr/bin/env python3
"""Serve the Wasm distribution with SPA fallback for deep links."""

import argparse
import http.server
import os
import socketserver


class SpaHandler(http.server.SimpleHTTPRequestHandler):
    def send_head(self):
        translated = self.translate_path(self.path)
        if not os.path.exists(translated):
            self.path = "/index.html"
        return super().send_head()

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=8123)
    parser.add_argument("--bind", default="0.0.0.0")
    parser.add_argument(
        "--directory",
        default="composeApp/build/dist/wasmJs/productionExecutable",
    )
    args = parser.parse_args()

    handler = lambda *a, **kw: SpaHandler(*a, directory=args.directory, **kw)
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.ThreadingTCPServer((args.bind, args.port), handler) as httpd:
        print(f"serving {args.directory} on http://{args.bind}:{args.port}", flush=True)
        httpd.serve_forever()


if __name__ == "__main__":
    main()
