-- Исправление типа колонки role с ENUM на VARCHAR
-- Если колонка имеет тип event_member_role (ENUM), изменяем на VARCHAR
DO $$
BEGIN
    -- Проверяем, существует ли тип event_member_role
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'event_members' 
        AND column_name = 'role' 
        AND data_type = 'USER-DEFINED'
    ) THEN
        -- Изменяем тип колонки с ENUM на VARCHAR
        ALTER TABLE event_members 
          ALTER COLUMN role TYPE VARCHAR(64) USING role::text;
    END IF;
END $$;

