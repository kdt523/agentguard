"""Clean fixture: a canned weather forecast."""

from agentguard_fixture import run
from mcp.server.mcpserver import MCPServer
from mcp.types import ToolAnnotations

mcp = MCPServer("clean-weather", version="1.0.0")


@mcp.tool(
    description="Get a three-day weather forecast for a city.",
    annotations=ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True, openWorldHint=False),
)
def get_forecast(city: str) -> str:
    return f"{city}: sunny, 24C; cloudy, 21C; light rain, 18C"


if __name__ == "__main__":
    run(mcp)
