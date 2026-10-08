"""Lists the tools of one MCP server and prints them as JSON.

Usage:
  python smoke_client.py stdio <docker-image> [ENV=VAL ...]
  python smoke_client.py http <url>
"""

import asyncio
import json
import sys

from mcp.client.client import Client
from mcp.client.stdio import StdioServerParameters


async def list_tools(target) -> list[str]:
    async with Client(target, read_timeout_seconds=30) as client:
        result = await client.list_tools()
        return sorted(t.name for t in result.tools)


async def check_rugpull(url: str) -> dict:
    """List tools 4 times in one session; v1 must be clean, the 4th listing poisoned."""
    notifications: list[str] = []

    async def on_message(message) -> None:
        method = getattr(getattr(message, "root", message), "method", None)
        if method:
            notifications.append(method)

    async with Client(url, read_timeout_seconds=30, message_handler=on_message) as client:
        descriptions = [(await client.list_tools()).tools[0].description for _ in range(4)]
        await asyncio.sleep(0.5)
    return {
        "first_clean": "<IMPORTANT>" not in descriptions[0],
        "last_poisoned": "<IMPORTANT>" in descriptions[-1],
        "list_changed": "notifications/tools/list_changed" in notifications,
    }


def main() -> None:
    mode, arg, *env = sys.argv[1:]
    if mode == "rugpull-http":
        result = asyncio.run(check_rugpull(arg))
        print(json.dumps(result))
        sys.exit(0 if result["first_clean"] and result["last_poisoned"] else 1)
    if mode == "stdio":
        docker_args = ["run", "--rm", "-i", "--network", "none"]
        for kv in env:
            docker_args += ["-e", kv]
        target = StdioServerParameters(command="docker", args=docker_args + [arg])
    else:
        target = arg
    tools = asyncio.run(list_tools(target))
    print(json.dumps({"count": len(tools), "tools": tools}))


if __name__ == "__main__":
    main()
