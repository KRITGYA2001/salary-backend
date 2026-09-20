-- Static exchange rates (USD per 1 unit of currency), dated. Editable without touching employee rows.
INSERT INTO currency (code, minor_unit_exponent, usd_per_unit, rate_as_of_date) VALUES
    ('USD', 2, 1.0,    '2026-01-01'),
    ('GBP', 2, 1.27,   '2026-01-01'),
    ('EUR', 2, 1.08,   '2026-01-01'),
    ('INR', 2, 0.012,  '2026-01-01'),
    ('JPY', 0, 0.0067, '2026-01-01'),
    ('CAD', 2, 0.73,   '2026-01-01'),
    ('AUD', 2, 0.65,   '2026-01-01'),
    ('SGD', 2, 0.74,   '2026-01-01'),
    ('BRL', 2, 0.18,   '2026-01-01');

INSERT INTO country (code, name, currency) VALUES
    ('US', 'United States',  'USD'),
    ('GB', 'United Kingdom', 'GBP'),
    ('DE', 'Germany',        'EUR'),
    ('FR', 'France',         'EUR'),
    ('IN', 'India',          'INR'),
    ('JP', 'Japan',          'JPY'),
    ('CA', 'Canada',         'CAD'),
    ('AU', 'Australia',      'AUD'),
    ('SG', 'Singapore',      'SGD'),
    ('BR', 'Brazil',         'BRL');

INSERT INTO department (name) VALUES
    ('Engineering'), ('Product'), ('Sales'), ('Marketing'),
    ('Human Resources'), ('Finance'), ('Operations'), ('Customer Support');

INSERT INTO job_title (title, department_id)
SELECT t.title, d.id FROM (
    SELECT 'Software Engineer' AS title, 'Engineering' AS dept UNION ALL
    SELECT 'Senior Software Engineer', 'Engineering' UNION ALL
    SELECT 'Engineering Manager',      'Engineering' UNION ALL
    SELECT 'QA Engineer',              'Engineering' UNION ALL
    SELECT 'DevOps Engineer',          'Engineering' UNION ALL
    SELECT 'Product Manager',          'Product' UNION ALL
    SELECT 'Product Designer',         'Product' UNION ALL
    SELECT 'Sales Executive',          'Sales' UNION ALL
    SELECT 'Account Manager',          'Sales' UNION ALL
    SELECT 'Sales Director',           'Sales' UNION ALL
    SELECT 'Marketing Specialist',     'Marketing' UNION ALL
    SELECT 'Content Strategist',       'Marketing' UNION ALL
    SELECT 'HR Generalist',            'Human Resources' UNION ALL
    SELECT 'Recruiter',                'Human Resources' UNION ALL
    SELECT 'Financial Analyst',        'Finance' UNION ALL
    SELECT 'Accountant',               'Finance' UNION ALL
    SELECT 'Operations Analyst',       'Operations' UNION ALL
    SELECT 'Operations Manager',       'Operations' UNION ALL
    SELECT 'Support Specialist',       'Customer Support' UNION ALL
    SELECT 'Support Team Lead',        'Customer Support'
) t JOIN department d ON d.name = t.dept;
