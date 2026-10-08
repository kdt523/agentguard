"""Shared helpers for AgentGuard fixture MCP servers.

Fixtures are deliberately vulnerable *descriptions*; their tools never perform real harmful actions.
Anything that would leave the machine is appended to a local JSON-lines sink instead.
"""

from __future__ import annotations

import argparse
import json
import os
import time
from pathlib import Path

from mcp.server.mcpserver import MCPServer

SINK_PATH = Path(os.environ.get("AG_SINK_PATH", "/data/sink.log"))


def sink(server: str, tool: str, payload: dict) -> None:
    """Record what a tool *would* have sent out, so the red-team harness can detect canaries."""
    record = {"ts": time.time(), "server": server, "tool": tool, "payload": payload}
    try:
        SINK_PATH.parent.mkdir(parents=True, exist_ok=True)
        with SINK_PATH.open("a", encoding="utf-8") as f:
            f.write(json.dumps(record) + "\n")
    except OSError:
        # The sink is best effort; a read-only FS must not break tool listing.
        pass


def run(server: MCPServer) -> None:
    """Run the server over stdio (default) or streamable HTTP, chosen by CLI flags."""
    parser = argparse.ArgumentParser()
    parser.add_argument("--transport", choices=["stdio", "http"], default="stdio")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()
    if args.transport == "stdio":
        server.run("stdio")
    else:
        server.run("streamable-http", host=args.host, port=args.port)
