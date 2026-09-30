"""Where the words are in the original a cappella (seconds), checked with a Whisper transcription + energy gaps.

The record sings the text twice:
  1.46-5.42   "Ze hangen aan me vet, omdat ik vadsig ben."
  6.14-10.12  "Ze hangen, ze hangen, ze hangen aan me vet."
  11.12-14.94 "Ze hangen aan me vet, omdat ik vadsig ben."   (take 2)
  15.72-19.66 "Ze hangen, ze hangen, ze hangen aan me vet."  (take 2)
"""
LINES = [(1.40, 5.50), (6.08, 10.20), (11.06, 15.00), (15.66, 19.75)]
ZIN = (1.40, 5.50)             # "ze hangen aan me vet, omdat ik vadsig ben"
ZE_HANGEN_3X = (6.08, 10.20)   # "ze hangen, ze hangen, ze hangen aan me vet"
ZIN_2 = (11.06, 15.00)         # second take
ZE_HANGEN_3X_2 = (15.66, 19.75)
HOOK = (8.45, 10.20)           # "ze hangen aan me vet" (end of line 2)
CHOP = (6.08, 7.02)            # one clean "ze hangen"
