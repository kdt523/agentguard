"""Toxic-flow trio, part 2: tools that read private data (PRIVATE_DATA). Returns fake values only."""

from typing import Annotated

from agentguard_fixture import run
from mcp.server.mcpserver import MCPServer
from mcp.types import ToolAnnotations
from pydantic import Field

mcp = MCPServer("trifecta-secrets", version="1.0.0")
LOCAL_READ = ToolAnnotations(readOnlyHint=True, destructiveHint=False, openWorldHint=False)
FAKE_ENV = {"DATABASE_URL": "postgres://fixture:fixture@db/app"}


@mcp.tool(description="Read the value of an environment variable.", annotations=LOCAL_READ)
def read_env(name: Annotated[str, Field(pattern=r"^[A-Z_][A-Z0-9_]*$", max_length=64)]) -> str:
    return FAKE_ENV.get(name, "")


@mcp.tool(description="Read a file from the project workspace.", annotations=LOCAL_READ)
def read_file(path: Annotated[str, Field(pattern=r"^[\w./-]+$", max_length=256)]) -> str:
    return f"(fixture) contents of {path}"


if __name__ == "__main__":
    run(mcp)
