-- AgentGuard initial schema (see agentguard-architecture skill)

CREATE TABLE servers (
    id          uuid PRIMARY KEY,
    key         text        NOT NULL UNIQUE,
    transport   text        NOT NULL,
    config      jsonb       NOT NULL DEFAULT '{}'::jsonb,
    third_party boolean     NOT NULL DEFAULT true,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE snapshots (
    id              uuid PRIMARY KEY,
    server_id       uuid        NOT NULL REFERENCES servers (id) ON DELETE CASCADE,
    fetched_at      timestamptz NOT NULL,
    server_name     text,
    server_version  text,
    snapshot_sha256 text        NOT NULL,
    raw             jsonb       NOT NULL
);
CREATE INDEX idx_snapshots_server_fetched ON snapshots (server_id, fetched_at DESC);

CREATE TABLE tools (
    id           uuid PRIMARY KEY,
    snapshot_id  uuid NOT NULL REFERENCES snapshots (id) ON DELETE CASCADE,
    name         text NOT NULL,
    description  text NOT NULL DEFAULT '',
    input_schema jsonb,
    annotations  jsonb,
    sha256       text NOT NULL
);
CREATE INDEX idx_tools_snapshot ON tools (snapshot_id);

CREATE TABLE scans (
    id              uuid PRIMARY KEY,
    status          text NOT NULL,
    target          jsonb,
    started_at      timestamptz,
    finished_at     timestamptz,
    score           integer,
    score_breakdown jsonb,
    error           text
);

CREATE TABLE findings (
    id          uuid PRIMARY KEY,
    scan_id     uuid    NOT NULL REFERENCES scans (id) ON DELETE CASCADE,
    check_id    text    NOT NULL,
    owasp_ref   text,
    severity    text    NOT NULL,
    score       integer NOT NULL,
    server_key  text,
    tool_name   text,
    evidence    jsonb,
    remediation text,
    confidence  numeric(4, 3)
);
CREATE INDEX idx_findings_scan ON findings (scan_id);

CREATE TABLE flows (
    id        uuid PRIMARY KEY,
    scan_id   uuid  NOT NULL REFERENCES scans (id) ON DELETE CASCADE,
    path      jsonb NOT NULL,
    risk      numeric,
    narrative text
);
CREATE INDEX idx_flows_scan ON flows (scan_id);

CREATE TABLE tool_pins (
    id          uuid PRIMARY KEY,
    server_key  text        NOT NULL,
    tool_name   text        NOT NULL,
    sha256      text        NOT NULL,
    approved_at timestamptz NOT NULL,
    approved_by text,
    UNIQUE (server_key, tool_name)
);

CREATE TABLE drift_events (
    id          uuid PRIMARY KEY,
    server_key  text        NOT NULL,
    tool_name   text,
    old_sha     text,
    new_sha     text,
    diff        jsonb,
    detected_at timestamptz NOT NULL
);

CREATE TABLE redteam_runs (
    id         uuid PRIMARY KEY,
    started_at timestamptz NOT NULL,
    model      text,
    config     jsonb,
    summary    jsonb
);

CREATE TABLE redteam_cases (
    id         uuid PRIMARY KEY,
    run_id     uuid NOT NULL REFERENCES redteam_runs (id) ON DELETE CASCADE,
    case_id    text NOT NULL,
    category   text,
    outcome    text,
    transcript jsonb,
    judge      jsonb
);
