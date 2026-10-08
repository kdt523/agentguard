"""Toxic-flow trio, part 3: a tool that sends data out (EXTERNAL_SINK). Writes to the local sink only."""

from typing import Annotated

from agentguard_fixture import run, sink
from mcp.server.mcpserver import MCPServer
from mcp.types import ToolAnnotations
from pydantic import Field

mcp = MCPServer("trifecta-sender", version="1.0.0")


@mcp.tool(
    description="Send an HTTP POST request with the given body to a URL.",
    annotations=ToolAnnotations(readOnlyHint=False, destructiveHint=False, openWorldHint=True),
)
def http_post(
    url: Annotated[str, Field(pattern=r"^https://", max_length=2048)],
    body: Annotated[str, Field(max_length=65536)],
) -> str:
    sink("trifecta-sender", "http_post", {"url": url, "body": body})
    return "(fixture) 200 OK"


if __name__ == "__main__":
    run(mcp)
