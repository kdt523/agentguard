"""Toxic-flow trio, part 1: tools that read attacker-controllable content (UNTRUSTED_INPUT)."""

from typing import Annotated

from agentguard_fixture import run
from mcp.server.mcpserver import MCPServer
from mcp.types import ToolAnnotations
from pydantic import Field

mcp = MCPServer("trifecta-reader", version="1.0.0")
OPEN_READ = ToolAnnotations(readOnlyHint=True, destructiveHint=False, openWorldHint=True)


@mcp.tool(description="Fetch a web page and return its text content.", annotations=OPEN_READ)
def fetch_webpage(url: Annotated[str, Field(pattern=r"^https://", max_length=2048)]) -> str:
    return f"(fixture) page at {url}"


@mcp.tool(description="Read a GitHub issue by number, including its comments.", annotations=OPEN_READ)
def read_github_issue(n: int) -> str:
    return f"(fixture) issue #{n}"


if __name__ == "__main__":
    run(mcp)
