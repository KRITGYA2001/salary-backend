CREATE TABLE currency (
    code                 TEXT PRIMARY KEY,
    minor_unit_exponent  INTEGER NOT NULL CHECK (minor_unit_exponent BETWEEN 0 AND 3),
    usd_per_unit         REAL    NOT NULL CHECK (usd_per_unit > 0),
    rate_as_of_date      TEXT    NOT NULL
);

CREATE TABLE country (
    code      TEXT PRIMARY KEY,
    name      TEXT NOT NULL UNIQUE,
    currency  TEXT NOT NULL REFERENCES currency (code)
);

CREATE TABLE department (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL UNIQUE
);

CREATE TABLE job_title (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    title          TEXT NOT NULL UNIQUE,
    department_id  INTEGER NOT NULL REFERENCES department (id)
);

CREATE TABLE employee (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_code    TEXT    NOT NULL UNIQUE,
    full_name        TEXT    NOT NULL,
    email            TEXT    NOT NULL UNIQUE,
    department_id    INTEGER NOT NULL REFERENCES department (id),
    job_title_id     INTEGER NOT NULL REFERENCES job_title (id),
    country_code     TEXT    NOT NULL REFERENCES country (code),
    employment_type  TEXT    NOT NULL CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT')),
    salary_minor     INTEGER NOT NULL CHECK (salary_minor > 0),
    hire_date        TEXT    NOT NULL,
    status           TEXT    NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at       TEXT    NOT NULL,
    updated_at       TEXT    NOT NULL
);

CREATE TABLE salary_history (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    employee_id       INTEGER NOT NULL REFERENCES employee (id),
    old_salary_minor  INTEGER,
    new_salary_minor  INTEGER NOT NULL CHECK (new_salary_minor > 0),
    currency          TEXT    NOT NULL REFERENCES currency (code),
    effective_date    TEXT    NOT NULL,
    reason            TEXT    NOT NULL,
    changed_at        TEXT    NOT NULL
);

CREATE INDEX idx_employee_country_department_title ON employee (country_code, department_id, job_title_id);
CREATE INDEX idx_employee_status                    ON employee (status);
CREATE INDEX idx_employee_full_name                 ON employee (full_name COLLATE NOCASE);
CREATE INDEX idx_employee_salary                    ON employee (salary_minor);
CREATE INDEX idx_salary_history_employee_changed    ON salary_history (employee_id, changed_at DESC);
