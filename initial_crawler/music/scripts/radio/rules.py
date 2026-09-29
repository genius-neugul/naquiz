"""docs/MUSIC_PARSING_RULE.md 1장(새 규칙)의 참조 구현. 문서 검증용."""
import re

# ---------- 공통 ----------
def normalize(s):
    s = s.replace("`", "'").replace("’", "'").replace("‘", "'").replace("“", '"').replace("”", '"').replace("（", "(").replace("）", ")").replace("［", "[").replace("］", "]")
    return " ".join(s.split())


def top_groups(s):
    """최상위 괄호 그룹 [(start, end, content)] 반환. 짝이 안 맞으면 끝까지."""
    out, depth, start = [], 0, None
    for i, ch in enumerate(s):
        if ch == "(":
            if depth == 0:
                start = i
            depth += 1
        elif ch == ")" and depth > 0:
            depth -= 1
            if depth == 0:
                out.append((start, i + 1, s[start + 1:i].strip()))
    if depth > 0 and start is not None:
        out.append((start, len(s), s[start + 1:].strip()))
    return out


def strip_groups(s):
    for a, b, _ in reversed(top_groups(s)):
        s = s[:a] + " " + s[b:]
    return " ".join(s.split()).strip()


# ---------- 제외 ----------
ORIG_TITLE = re.compile(r"^\s*원제\s*:\s*(.+)$")
SIGNAL = re.compile(r"타이틀 뮤직|(?<![A-Za-z])BGM(?![A-Za-z])|시그널 (뮤직|송)|퀴즈용|로고송", re.I)
INST = re.compile(r"(?<![A-Za-z])(inst|instrumental|mr)(?![A-Za-z.])|(?<![A-Za-z])inst\.|반주", re.I)

# ---------- 부가정보 키워드 ----------
INFO_WORDS = [
    "feat", "featuring", "ft", "prod", "produced", "remaster", "remastered", "mastered", "live", "inst", "instrumental",
    "mr", "remix", "mix", "ver", "version", "edit", "album", "single", "original", "radio", "extended",
    "acoustic", "lp", "ep", "explicit", "clean", "bonus", "digital", "mono", "stereo", "duet", "narr",
    "narration", "vocal", "vocals", "cf", "ost", "theme", "soundtrack", "demo", "intro",
    "outro", "reprise", "unplugged", "session", "track", "cover", "bgm", "english", "stage",
    "japanese", "korean", "chinese", "piano", "orchestra", "orchestral", "sped", "slowed", "rap", "unit",
]
INFO_KO = ["영화", "드라마", "광고", "삽입곡", "원곡", "원제", "편곡", "버전", "라이브", "테마", "시그널",
           "타이틀", "퀴즈", "반주", "오프닝", "엔딩", "주제가", "방송", "리메이크", "리믹스", "연주", "합창",
           "듀엣", "보컬", "나레이션", "OST"]
INFO_RE = re.compile(r"(?<![A-Za-z가-힣])(" + "|".join(INFO_WORDS) + r")(?![A-Za-z가-힣])\.?", re.I)
# 한국어 키워드는 부분 문자열로 찾는다. 단 '라이브'는 '드라이브' 안에 있으면 제외
INFO_KO_RE = re.compile("|".join("(?<!드)라이브" if k == "라이브" else re.escape(k) for k in INFO_KO), re.I)
YEAR_RE = re.compile(r"(?<!\d)(19|20)\d{2}(?!\d)")


# 흔한 영어 단어라 괄호 내용의 맨 앞에 올 때만 부가정보로 본다: (with 아이유), (From "Frozen"), (Part 2)
INFO_START_RE = re.compile(r"^(with|from|part|pt|for|by|in the style of)(?![A-Za-z])", re.I)


def is_info(c):
    c = c.strip()
    if not c:
        return True
    return bool(INFO_RE.search(c) or INFO_START_RE.search(c) or re.search(r"\s시$", c)
                or YEAR_RE.search(c) or INFO_KO_RE.search(c))


# ---------- 제목 ----------
def parse_title(raw):
    """(대표 제목, [별칭], 제외 사유) 반환. 제외면 대표 제목은 None."""
    t = normalize(raw)
    if t.startswith("*"):
        return None, [], "메들리(*로 시작)"
    groups_raw = [c for _, _, c in top_groups(t.replace("[", "(").replace("]", ")"))]
    star = t.split("*", 1)[1] if "*" in t else ""
    if any(SIGNAL.search(c) for c in groups_raw):
        return None, [], "시그널/BGM 표기"
    if any(INST.search(c) for c in groups_raw) or INST.search(star):
        return None, [], "Inst/MR"
    t = t.split("*", 1)[0].strip()                      # * 이후 제거
    t = t.replace("[", "(").replace("]", ")")            # 대괄호 = 소괄호
    aliases = []
    groups = top_groups(t)
    lead = groups[0] if groups and groups[0][0] == 0 else None
    if lead and not is_info(lead[2]):                    # 맨 앞 괄호는 제목의 일부
        aliases.append(" ".join((lead[2] + " " + strip_groups(t[lead[1]:])).split()))
    for a, b, c in groups:
        if (a, b, c) == lead:
            continue
        m = ORIG_TITLE.match(c)
        if m:                                            # (원제: X) → X는 별칭
            aliases.append(m.group(1).strip())
        elif not is_info(c):
            aliases.append(c)
    title = strip_groups(t)
    title = FEAT_RE.sub("", title).strip()              # 괄호 밖 feat. 이후 제거
    if " - " in title:                                   # 대시 뒤가 부가정보면 제거
        left, right = [p.strip(" -") for p in title.split(" - ", 1)]
        if is_info(right) and left:
            title = left
    title = title.strip(" -")
    if not title and len(groups) == 1 and not is_info(groups[0][2]):
        title = groups[0][2]                             # 제목 전체가 괄호: (Nice Dream)
        aliases = []
    if not title:
        return None, [], "빈 제목"
    aliases = [x for x in dict.fromkeys(aliases) if x and x.lower() != title.lower()]
    return title, aliases, None


# ---------- 아티스트 ----------
NO_ARTIST = {"", "various artists", "various artist", "v.a", "v.a.", "va", "ost", "unknown"}
ROLES = ["VOCALS", "VOCAL", "PRODUCER", "PRODUCE", "PIANO", "GUITAR", "BASS", "SAXOPHONE", "TENOR SAXOPHONE",
         "ALTO SAXOPHONE", "VIOLIN", "TRUMPET", "FLUGELHORN", "SOLO", "NARRATION", "VERSE", "MIX", "DRUMS",
         "CELLO", "BANDONEON", "TENOR", "RAP", "CHORUS", "DIR"]
ROLE_RE = re.compile(r"\s*:\s*(" + "|".join(sorted(ROLES, key=len, reverse=True)) + r")(?![A-Za-z])", re.I)
TAIL_RE = re.compile(r"(?<=\S)\s+(live|narr\.?|duet)(?![A-Za-z]).*$", re.I)
FEAT_RE = re.compile(r"\s*(?<![A-Za-z])(feat\.|ft\.|featuring(?![A-Za-z])|feat(?=\s|\())(.*)", re.I)
CREDIT_PREFIX = re.compile(r"^\s*(원곡|노래|작곡|작사|편곡)\s*:\s*")
SEP_RE = re.compile(r"\s*,\s*|\s*&\s*|\s+\+\s+|\s+(?:and|x|with|vs\.?|×)\s+", re.I)
OF_RE = re.compile(r"\s+(?:of|from)\s+", re.I)
HANGUL = re.compile(r"[가-힣]")
SPECIAL = {"체리 필터": "체리필터"}


def split_top(s, regex):
    """괄호 밖에서만 regex로 나눈다."""
    mask = list(s)
    for a, b, _ in top_groups(s):
        for i in range(a, b):
            mask[i] = "\0"
    masked = "".join(mask)
    parts, last = [], 0
    for m in regex.finditer(masked):
        parts.append(s[last:m.start()])
        last = m.end()
    parts.append(s[last:])
    return [p.strip() for p in parts if p.strip()]


def clean(s):
    return re.sub(r"\s+,", ",", strip_groups(s)).strip(" ,")


def names_of(piece):
    """'A (B)' → 한글 이름 먼저 [A, B]. 괄호 안이 멤버 목록(구분자 포함)이면 별칭으로 쓰지 않는다."""
    outer = clean(piece)
    inner = [c for _, _, c in top_groups(piece) if not is_info(c) and len(split_top(c, SEP_RE)) == 1]
    names = [outer] + inner
    names = [n for n in names if n]
    return sorted(names, key=lambda n: 0 if HANGUL.search(n) else 1)


def parse_artist(raw):
    """아티스트 후보 목록. 빈 목록 = 아티스트 없음."""
    a = normalize(raw).replace("[", "(").replace("]", ")").lstrip("/ ").strip()
    if a.lower() in NO_ARTIST:
        return []
    a = re.sub(r"^OST\s*/\s*", "", a, flags=re.I)                 # OST / X
    a = split_top(a, re.compile(r"\s+/\s+"))[0] if a else a       # 'A / B' → A
    a = CREDIT_PREFIX.sub("", a)
    # 괄호 안 부가정보(feat, 원곡 …) 제거
    for s_, e_, c in reversed(top_groups(a)):
        if is_info(c):
            a = a[:s_] + " " + a[e_:]
    a = " ".join(a.split())
    a = split_top(a, FEAT_RE)[0] if split_top(a, FEAT_RE) else ""
    a = TAIL_RE.sub("", a)                                        # 뒤에 붙은 live, narr., duet
    a = ROLE_RE.sub("", a).strip(" ,")
    if a.lower() in NO_ARTIST:
        return []
    parts = split_top(a, SEP_RE)
    first = parts[0] if parts else a
    of = split_top(first, OF_RE)
    if len(parts) == 1 and len(of) == 1:
        cands = names_of(a)                                       # 한 명: 'A (B)' → 한글 먼저
    else:
        cands = [clean(a)]                                        # 전체(팀 이름일 수 있음)
        cands += names_of(of[0])                                  # 첫 아티스트, 'A of 그룹'이면 A
    cands = [SPECIAL.get(c, c) for c in cands]
    return [c for c in dict.fromkeys(c.strip(" ,") for c in cands) if c]


# ================= final 형식 (title/subtitle, artist/artist_sub) =================
import csv as _csv
import os as _os

ROLES.append("SAX")
ROLE_RE = re.compile(r"\s*:\s*(" + "|".join(sorted(ROLES, key=len, reverse=True)) + r")(?![A-Za-z])", re.I)


def _load_special():
    path = _os.path.join(_os.path.dirname(_os.path.abspath(__file__)), "special_artists.csv")
    with open(path, encoding="utf-8") as f:
        # 표의 ' (&|and) '는 공백 없는 & 도 허용한다(Belle&Sebastian)
        return [(re.compile(r"(?<![A-Za-z0-9가-힣])" + r["pattern"].replace(" (&|and) ", r"(?:\s*&\s*|\s+and\s+)")
                            + r"(?![A-Za-z0-9가-힣])", re.I),
                 r["artist"], r["artist_sub"]) for r in _csv.DictReader(f)]


SPECIAL_ARTISTS = _load_special()
PLACEHOLDER = re.compile(r"§(\d+)§")
SINGER_ROLE = re.compile(r":\s*(VOCALS?|SOLO|RAP)(?![A-Za-z])", re.I)

# 여러 명을 나누는 구분자. 쉼표 뒤 Jr는 이름의 일부(Leslie Odom, Jr)
FINAL_SEP = re.compile(
    r"\s*,(?!\s*Jr\b)\s*"
    r"|\s*&\s*"
    r"|\s+(?:and|x|with|vs\.?|×)\s+"
    r"|\s*\+\s*"
    r"|(?<=[가-힣])[xX](?=[가-힣])",          # 김사월X김해원, 도겸X승관
    re.I)
# 'A & The B', 'A and His Orchestra', 'A & Friends' 는 한 팀 이름
TEAM_TAIL = re.compile(r"^(the|his|her)\s|^(sons|friends|papas|chorus)$", re.I)
# 위 형태지만 실제로는 두 팀·사람의 협업인 경우
NOT_TEAM_TAIL = {"the chainsmokers", "the fat boys", "the police", "the edge"}


def has_hangul(s):
    return bool(HANGUL.search(s))


def parse_title_fields(raw):
    """(title, subtitle, 제외 사유). 한글이 있는 쪽이 title, 다른 쪽이 subtitle."""
    t, aliases, why = parse_title(raw)
    if why:
        return None, None, why
    alias = aliases[0] if aliases else ""
    if alias and has_hangul(alias) and not has_hangul(t):
        return alias, t, None
    return t, alias, None


def _split_with_seps(s):
    """괄호 밖에서 FINAL_SEP으로 나누고 (조각, 앞 구분자) 목록을 돌려준다."""
    mask = list(s)
    for a, b, _ in top_groups(s):
        for i in range(a, b):
            mask[i] = "\0"
    masked = "".join(mask)
    out, last, sep = [], 0, ""
    for m in FINAL_SEP.finditer(masked):
        out.append((s[last:m.start()].strip(), sep))
        sep, last = m.group(0), m.end()
    out.append((s[last:].strip(), sep))
    return [(p, sp) for p, sp in out if p]


def _join_team_tails(parts):
    """'A & The B' 처럼 뒤 조각이 팀 이름 꼬리이면 앞 조각과 다시 붙인다."""
    merged = []
    for piece, sep in parts:
        if merged and sep.strip().lower() in ("&", "and") and TEAM_TAIL.search(piece) \
                and piece.lower() not in NOT_TEAM_TAIL:
            merged[-1] = merged[-1] + " " + sep.strip() + " " + piece
        else:
            merged.append(piece)
    return merged


PERSON_NAME = re.compile(r"^[가-힣]{2,3}$")


def name_key(s):
    return re.sub(r"[\W_]+", "", s.lower()) or s.lower().strip()


class ArtistIndex:
    """데이터 전체에서 '혼자 적힌' 아티스트 이름과 그 병기 이름을 모은 색인.

    - 'A of B'에서 B가 혼자 적힌 아티스트로 있으면 멤버 of 그룹으로 본다(없으면 KISS OF LIFE 같은 팀 이름).
    - 'A (B)'에서 B의 병기 이름이 따로 있는데 A가 아니면 B는 소속 그룹이다(부석순 (SEVENTEEN)).
      B에 병기 이름이 없어도, A가 2~3글자 한글 이름이고 B가 혼자 적힌 곡이 3곡 이상이면 소속 그룹이다(산들 (B1A4)).
    - 이름 비교는 대소문자·공백·문장부호를 무시한다(FT ISLAND = FTISLAND).
    """

    def __init__(self, raw_artists=()):
        self.names = {}
        self.counts = {}
        for raw in raw_artists:
            a = normalize(raw).replace("[", "(").replace("]", ")").lstrip("/ ")
            a = re.sub(r"^OST\s*/\s*", "", a, flags=re.I)
            parts = split_top(a, FEAT_RE)
            a = ROLE_RE.sub("", parts[0] if parts else "").strip(" ,")
            if not a or len(split_top(a, FINAL_SEP)) > 1 or len(split_top(a, OF_RE)) > 1:
                continue
            outer = name_key(clean(a))
            aliases = {name_key(c) for _, _, c in top_groups(a) if not is_info(c)}
            self.names.setdefault(outer, set()).update(aliases)
            self.counts[outer] = self.counts.get(outer, 0) + 1

    def known(self, name):
        return name_key(name) in self.names

    def is_affiliation(self, name, alias):
        n, a = name_key(name), name_key(alias)
        own = self.names.get(a, set())
        if own:  # B가 자기 병기 이름을 따로 가지고 있는데(SEVENTEEN (세븐틴)) 거기에 A가 없다
            return n not in own
        # 한글 사람 이름 (영문 그룹): 짧은 한글 이름이고 영문이 혼자 적힌 곡이 3곡 이상이면 소속 그룹이다
        return (has_hangul(name) and not has_hangul(alias) and PERSON_NAME.match(name.strip()) is not None
                and self.counts.get(a, 0) >= 3)


def _piece_fields(piece, specials, index):
    """조각 하나 → (artist, artist_sub). 이름이 없으면 None."""
    of = split_top(piece, OF_RE)
    if len(of) > 1:
        member, group = of[0], clean(PLACEHOLDER.sub("", of[1])) if not PLACEHOLDER.search(of[1]) else ""
        if has_hangul(clean(of[0])) or has_hangul(group) or PLACEHOLDER.search(of[1]) or index.known(group):
            piece = member                                   # 멤버 of 그룹 → 멤버
    outer = clean(piece)
    groups = [c for _, _, c in top_groups(piece)]
    specials_in = [specials[int(m.group(1))] for c in [outer] + groups for m in PLACEHOLDER.finditer(c)]
    if PLACEHOLDER.search(outer):                            # 특별 케이스 이름
        _, artist, sub = specials_in[0]
        return artist, sub
    for _, artist, sub in specials_in:                       # 에프엑스(f(x)): 괄호 안이 같은 팀의 특별 케이스
        if outer.lower() in (artist.lower(), sub.lower()):
            return artist, sub
    inner = [c for c in groups if not is_info(c) and len(split_top(c, FINAL_SEP)) == 1 and not PLACEHOLDER.search(c)]
    if not outer:
        return None
    alias = inner[0].split("/")[0].strip() if inner else ""  # (프리템포/Takeshi Hanzawa) → 프리템포
    if alias and (index.is_affiliation(outer, alias) or (has_hangul(alias) and has_hangul(outer))):
        alias = ""                                           # 괄호 안이 소속 그룹
    if alias and has_hangul(alias) and not has_hangul(outer):
        return SPECIAL.get(alias, alias), outer
    return SPECIAL.get(outer, outer), alias or SPECIAL.get(outer, outer)


EMPTY_INDEX = ArtistIndex()


def parse_artist_fields(raw, index=EMPTY_INDEX):
    """원문 아티스트 → (artist 목록, artist_sub 목록). 아티스트가 없으면 ([], [])."""
    a = normalize(raw).replace("[", "(").replace("]", ")").lstrip("/ ").strip()
    if a.lower() in NO_ARTIST:
        return [], []
    specials = []

    def protect(m, entry):
        specials.append(entry)
        return f"§{len(specials) - 1}§"

    for regex, artist, sub in SPECIAL_ARTISTS:
        a = regex.sub(lambda m, e=(regex, artist, sub): protect(m, e), a)

    a = re.sub(r"^OST\s*/\s*", "", a, flags=re.I)
    slash = split_top(a, re.compile(r"\s+/\s+")) if a else [a]
    # '이름: 역할 / 이름: 역할'이면 부른 사람(VOCAL, SOLO 등)을, 아니면 앞 조각을 쓴다
    a = next((p for p in slash if SINGER_ROLE.search(p)), slash[0]) if slash else a
    a = CREDIT_PREFIX.sub("", a)
    for s_, e_, c in reversed(top_groups(a)):
        if is_info(c) and not PLACEHOLDER.search(c):
            a = a[:s_] + " " + a[e_:]
    a = " ".join(a.split())
    parts = split_top(a, FEAT_RE)
    a = parts[0] if parts else ""
    a = TAIL_RE.sub("", a)
    a = ROLE_RE.sub("", a).strip(" ,")
    if a.lower() in NO_ARTIST:
        return [], []

    artists, subs = [], []
    for piece in _join_team_tails(_split_with_seps(a)):
        piece = ROLE_RE.sub("", piece).strip(" ,")
        f = _piece_fields(piece, specials, index)
        if f and f[0] and f[0] not in artists:
            artists.append(f[0])
            subs.append(f[1])
    return artists, subs
