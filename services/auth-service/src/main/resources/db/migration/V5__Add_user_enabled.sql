-- GDPR / account lifecycle: block login after erasure request
ALTER TABLE "USER" ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;
