"""Clean fixture: read-only access to a small in-memory notebook."""

from agentguard_fixture import run
from mcp.server.mcpserver import MCPServer
from mcp.types import ToolAnnotations

mcp = MCPServer("clean-notes", version="1.0.0")

NOTES = {
    "groceries": "Milk, eggs, bread.",
    "ideas": "Write a blog post about unit testing.",
}
READ_ONLY = ToolAnnotations(readOnlyHint=True, destructiveHint=False, idempotentHint=True, openWorldHint=False)


@mcp.tool(description="List the titles of all notes.", annotations=READ_ONLY)
def list_notes() -> list[str]:
    return sorted(NOTES)


@mcp.tool(description="Return the text of the note with the given title.", annotations=READ_ONLY)
def read_note(title: str) -> str:
    return NOTES.get(title, "")


if __name__ == "__main__":
    run(mcp)
