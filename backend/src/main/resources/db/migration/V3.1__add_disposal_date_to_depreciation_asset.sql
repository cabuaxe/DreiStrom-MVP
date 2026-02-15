-- ============================================================
-- DreiStrom MVP – Add disposal_date to depreciation_asset (V3.1)
-- Tracks when an asset was disposed (sold/scrapped)
-- ============================================================

ALTER TABLE depreciation_asset
    ADD COLUMN disposal_date DATE NULL COMMENT 'Date the asset was disposed (sold/scrapped)' AFTER expense_entry_id;
