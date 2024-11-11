CREATE OR REPLACE FUNCTION update_embeddings_delayed(start_id INTEGER, end_id INTEGER)
RETURNS TEXT AS $$
DECLARE
  rec RECORD; -- Declare a record variable to hold each row retrieved from the apparels table
BEGIN
  -- Iterate through each row in the apparels table where the id is between start_id and end_id
  FOR rec IN SELECT * FROM apparels WHERE id BETWEEN start_id AND end_id LOOP
    -- Update the embedding field for the current row based on its pdt_desc
    UPDATE apparels SET embedding = embedding('text-embedding-004', rec.pdt_desc)
    WHERE apparels.id = rec.id;
    -- Simulate a 1-second delay between each update
    PERFORM pg_sleep(1);
  END LOOP;

  -- Return a string message indicating the range of IDs that were updated
  RETURN 'Embeddings updated for IDs between ' || start_id || ' and ' || end_id; 
END;
$$ LANGUAGE plpgsql;

-- Sample runs
SELECT update_embeddings_delayed(1, 20);