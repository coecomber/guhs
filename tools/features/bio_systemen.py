"""
biomes3 slice "systemen" (Java: feature/bio/systemen; English: tools/lang/en/c90_bio_systemen.json; contract: CONTRACT_BIO.md).

Closing: FTB, Reisbureau destinations, titles, Guhdex. A stub until its slice fills it in.

It is the LAST biomes3 module: its build ends with the kern's check over everything the slices made (keep that line).
"""
from features import bio


def build(h):
    selfcheck(h)
    bio.selfcheck(h)


def selfcheck(h):
    pass
