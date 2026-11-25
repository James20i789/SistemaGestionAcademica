ALTER TABLE grade ALTER COLUMN score DROP NOT NULL;

CREATE INDEX grade_student ON grade(student_id, course_id);
