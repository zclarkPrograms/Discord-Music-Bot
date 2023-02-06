import beatmachine as bm
import sys

filename=sys.argv[1]

beats = bm.Beats.from_song(filename)
beats.apply(bm.effects.RemoveEveryNth(period=2)).save(filename.replace(".mp3", "_edited.mp3"))
print("done")