# 1.6.4: self-made choices (verblijfsbord family dialog)

1. **Male/female choice:** only for species with their own `_man` model or texture, read from the mod's files so it works on both server and client. That is 16 species:
   - dikdik, gouden dikdik, edelhert, giraffe, ijsbeer, kafferbuffel, leeuw, luipaard;
   - mammoet, narwal, neushoorn, wolharige neushoorn, nijlpaard, olifant, orang-oetan, prieelvogel.
   - All other species only get the parent/child choice.
2. **Placing first:** the standard family (male, female, 2 little ones) appears right away and the dialog opens by itself. Changed members are swapped with a poof. Alternative: no animals until you confirm.
3. **At least one parent:** four little ones alone is refused (the button is grey and says why). Alternative: allow any combination.
4. **Same-looking species:**
   - a parent internally keeps its old role (male/female);
   - a new parent becomes a female first, so the ostrich egg (only from a "vrouw") keeps working;
   - after that it becomes a male.
5. **Names:** they stay with the place in the family, even when the animal is swapped (the new little one is called "Simba" again). Alternative: clear the name when the role changes.
6. **Terrarium:** the dialog only shows the places that live in the tank (female + little one, or all 4 for a group). The empty places stay as they are.
7. **Cancelling:**
   - the first time the button says "Zo laten", later "Annuleren"; the family stays as it is;
   - "Familie samenstellen..." on the Familie tab opens the dialog again.
8. **Boards from before 1.6.4:** keep their family (roles missing → the standard), count as "already chosen", and get no dialog by themselves.
9. **The dialog is part of the verblijfsbord screen** (the same panel, colours and tooltips), not a separate window.
