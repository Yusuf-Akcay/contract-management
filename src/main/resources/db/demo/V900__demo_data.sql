INSERT INTO customer (customer_number, first_name, last_name, date_of_birth, email, version, created_at, updated_at, created_by, updated_by) VALUES
    ('C-100001', 'Erika', 'Mustermann', DATE '1985-04-12', 'erika@example.com', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'demo', 'demo'),
    ('C-100002', 'Max', 'Schmidt', DATE '1979-11-03', 'max@example.com', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'demo', 'demo');

INSERT INTO contract (contract_number, customer_id, line_of_business, premium, start_date, end_date, status, version, created_at, updated_at, created_by, updated_by)
SELECT v.nr, c.id, v.line_of_business, v.premium, v.start_date, v.end_date, v.status, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'demo', 'demo'
FROM (VALUES
        ('P-20260001', 'C-100001', 'HOUSEHOLD', 12.90, DATE '2026-01-01', CAST(NULL AS DATE), 'ACTIVE'),
        ('P-20260002', 'C-100001', 'MOTOR', 54.30, DATE '2025-03-01', CAST(NULL AS DATE), 'ACTIVE'),
        ('P-20260003', 'C-100002', 'LIABILITY', 6.50, DATE '2024-07-01', DATE '2026-06-30', 'CANCELLED')
     ) AS v (nr, customer_number, line_of_business, premium, start_date, end_date, status)
JOIN customer c ON c.customer_number = v.customer_number;
