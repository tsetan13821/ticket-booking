-- Sample shows, loaded on every startup after Hibernate creates the tables.
-- Every statement is idempotent, so restarting the app does not duplicate data.

INSERT INTO shows (movie_name, venue, start_time)
SELECT 'Interstellar', 'Galaxy Cinemas - Screen 1', CURRENT_DATE + INTERVAL '7 days' + TIME '18:30'
WHERE NOT EXISTS (SELECT 1 FROM shows WHERE movie_name = 'Interstellar' AND venue = 'Galaxy Cinemas - Screen 1');

INSERT INTO shows (movie_name, venue, start_time)
SELECT 'Dune: Part Two', 'Starlight Multiplex - Screen 3', CURRENT_DATE + INTERVAL '8 days' + TIME '21:00'
WHERE NOT EXISTS (SELECT 1 FROM shows WHERE movie_name = 'Dune: Part Two' AND venue = 'Starlight Multiplex - Screen 3');

-- Rows A-E x 10 seats for each sample show.
INSERT INTO seats (show_id, seat_number, price, status)
SELECT s.id, r.row_letter || n.seat_no, 250.00, 'AVAILABLE'
FROM shows s
CROSS JOIN (VALUES ('A'), ('B'), ('C'), ('D'), ('E')) AS r(row_letter)
CROSS JOIN generate_series(1, 10) AS n(seat_no)
WHERE (s.movie_name, s.venue) IN (('Interstellar', 'Galaxy Cinemas - Screen 1'),
                                  ('Dune: Part Two', 'Starlight Multiplex - Screen 3'))
ORDER BY s.id, r.row_letter, n.seat_no
ON CONFLICT (show_id, seat_number) DO NOTHING;
