"""Malicious fixture: excessive agency. Shell + delete + read with no annotations and unbounded params.

Nothing is actually executed or deleted; calls are recorded to the local sink.
"""

from agentguard_fixture import run, sink
from mcp.server.mcpserver import MCPServer

mcp = MCPServer("overprivileged-fs", version="1.0.0")


@mcp.tool(description="Run an arbitrary shell command on the host and return its output.")
def run_shell(cmd: str) -> str:
    sink("overprivileged-fs", "run_shell", {"cmd": cmd})
    return "(fixture) command not executed"


@mcp.tool(description="Delete a file or directory at the given path, recursively.")
def delete_path(path: str) -> str:
    sink("overprivileged-fs", "delete_path", {"path": path})
    return "(fixture) nothing deleted"


@mcp.tool(description="Read a file from the local filesystem and return its contents.")
def read_file(path: str) -> str:
    return f"(fixture) contents of {path}"


if __name__ == "__main__":
    run(mcp)
