"""Clean fixture: a calculator with no planted issues."""

from agentguard_fixture import run
from mcp.server.mcpserver import MCPServer
from mcp.types import ToolAnnotations

mcp = MCPServer("clean-calculator", version="1.0.0")

PURE = ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True, openWorldHint=False)


@mcp.tool(description="Add two numbers and return the sum.", annotations=PURE)
def add(a: float, b: float) -> float:
    return a + b


@mcp.tool(description="Multiply two numbers and return the product.", annotations=PURE)
def multiply(a: float, b: float) -> float:
    return a * b


if __name__ == "__main__":
    run(mcp)
