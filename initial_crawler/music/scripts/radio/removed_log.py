"""radio_songs_removed.csv 기록 도우미. 지운 곡을 원문 (제목, 가수)별로 모아 사유와 함께 남긴다."""
import collections
import csv
import os

FIELDS = ["stage", "reason", "radio", "radio_name", "seq", "date", "title", "artist", "count"]


def write(path, stage, items):
    """items: [(reason, row)] — row는 radio, radio_name, seq, date, title, artist를 가진 dict.

    같은 stage의 기존 기록은 지우고 다시 쓴다(다른 stage 기록은 유지).
    """
    kept = []
    if os.path.exists(path):
        with open(path, encoding="utf-8-sig") as f:
            kept = [r for r in csv.DictReader(f) if r["stage"] != stage]

    groups = collections.OrderedDict()
    for reason, row in items:
        k = (reason, row["title"], row["artist"])
        cnt, latest = groups.get(k, (0, None))
        if latest is None or (row["date"], int(row["seq"])) > (latest["date"], int(latest["seq"])):
            latest = row
        groups[k] = (cnt + 1, latest)

    new = [{"stage": stage, "reason": reason, "radio": r["radio"], "radio_name": r["radio_name"],
            "seq": r["seq"], "date": r["date"], "title": title, "artist": artist, "count": cnt}
           for (reason, title, artist), (cnt, r) in groups.items()]
    new.sort(key=lambda r: (r["reason"], -int(r["count"]), r["title"]))

    stage_order = {"unique": 0, "final": 1}
    rows = sorted(kept + new, key=lambda r: stage_order.get(r["stage"], 9))
    with open(path, "w", newline="", encoding="utf-8-sig") as f:
        w = csv.DictWriter(f, fieldnames=FIELDS)
        w.writeheader()
        w.writerows(rows)
    return collections.Counter(reason for reason, _ in items)
