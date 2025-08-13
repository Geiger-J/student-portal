-- -----------------------------------------------------------------------------
-- Seed data for Subjects and Timeslots
-- This runs at application startup (when using create/update schema strategy).
-- -----------------------------------------------------------------------------

-- Subjects
INSERT INTO subjects (name) VALUES
  ('Mathematics') ON CONFLICT DO NOTHING;
INSERT INTO subjects (name) VALUES
  ('Physics') ON CONFLICT DO NOTHING;
INSERT INTO subjects (name) VALUES
  ('Chemistry') ON CONFLICT DO NOTHING;
INSERT INTO subjects (name) VALUES
  ('Biology') ON CONFLICT DO NOTHING;
INSERT INTO subjects (name) VALUES
  ('English Literature') ON CONFLICT DO NOTHING;
INSERT INTO subjects (name) VALUES
  ('History') ON CONFLICT DO NOTHING;
INSERT INTO subjects (name) VALUES
  ('Geography') ON CONFLICT DO NOTHING;
INSERT INTO subjects (name) VALUES
  ('Computer Science') ON CONFLICT DO NOTHING;

-- Timeslots (Mon-Fri Period 1-7)
DO $$
BEGIN
  FOR d IN ARRAY['Monday','Tuesday','Wednesday','Thursday','Friday'] LOOP
    FOR p IN 1..7 LOOP
      INSERT INTO timeslots (label) VALUES (d || ' Period ' || p)
      ON CONFLICT DO NOTHING;
    END LOOP;
  END LOOP;
END$$;