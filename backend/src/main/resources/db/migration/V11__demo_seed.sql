INSERT INTO public.users (username, email, role)
SELECT 'organizer', 'organizer@musclub.local', 'ORGANIZER'
WHERE NOT EXISTS (
  SELECT 1
  FROM public.users
  WHERE username = 'organizer' OR email = 'organizer@musclub.local'
);

INSERT INTO public.users (username, email, role)
SELECT 'member', 'member@musclub.local', 'MEMBER'
WHERE NOT EXISTS (
  SELECT 1
  FROM public.users
  WHERE username = 'member' OR email = 'member@musclub.local'
);
