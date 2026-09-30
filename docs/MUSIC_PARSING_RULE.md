# 노래 정보 파싱 규칙

MBC 라디오 선곡표 전체 회차를 크롤링하고, 가사 없는 곡을 지우고, 같은 곡을 합치고, 제목·가수를 파싱해 최종 노래 목록(`radio_songs_final.json`)을 만드는 규칙이다. 특정 구현과 상관없이 같은 데이터 소스를 다룰 때 참고할 수 있도록 썼다.

- 게임에서 곡 정보를 조회하고 재생 영상을 고르는 규칙: [`MUSIC_SELECTION_RULE.md`](MUSIC_SELECTION_RULE.md)
- 실행 방법과 옵션: [`initial_crawler/README.md`](../initial_crawler/README.md)
- 크롤링 결과 수치: [`initial_crawler/records/`](../initial_crawler/records/)

---

## 0. 전체 흐름

라디오 선곡표는 게임 중에 조회하지 않는다. **초기 데이터 세팅**에서는 전체 회차를 미리 모아 **최종 노래 목록**을 만들어 두고, 게임은 그 목록에서 곡을 고른다.

그 뒤 새 방송은 [데일리 크롤링](DOMAIN.md#3-8-크롤링-실행-애그리거트)이 매일 추가한다. 같은 정제 규칙(1-4~1-9)을 쓰고, 이미 있는 곡(제목·가수 키가 같은 곡)은 추가하지 않는다.

```
1-2 전체 회차 크롤링           scripts/radio/crawl.py        → radio_songs.csv
1-4 가사 없는 곡 제거 ┐
1-5 같은 곡 하나로 합치기 ┘   scripts/radio/make_unique.py  → radio_songs_unique.csv
1-6~1-9 제목·가수 정제          scripts/radio/make_final.py → radio_songs_final.json
      (삭제한 곡 → radio_songs_removed.csv, 규칙으로 처리 못한 곡 → radio_songs_unresolved.csv)
```

- 최종 목록은 JSON(UTF-8)이고, 나머지 결과 파일은 UTF-8(BOM 포함) CSV다. 최종 목록을 JSON으로 두는 이유는 가수가 여러 명일 때 배열로 담기 위해서다. CSV 한 칸에 쉼표로 이으면 `Earth, Wind & Fire` 같은 이름과 구분되지 않는다.
- 같은 입력이면 항상 같은 결과가 나온다.

---

## 1. Radio (MBC 선곡표 → 최종 노래 목록)

아래 순서대로 처리한다.

### 1-1. 데이터 소스와 회차

- 선곡표: `https://miniweb.imbc.com/Music/View?seqID={회차}&progCode={프로그램 코드}`
- 회차 목록: `https://miniweb.imbc.com/Music?progCode={프로그램 코드}`. 목록의 첫 번째 `seqID`가 최신 회차다.

| 프로그램 (`radio`) | progCode | 크롤링 범위 (seqID) |
|---|---|---|
| 정오의 희망곡 김신영입니다 (`MUSIC_PARTY`) | `FM4U000001226` | 3000 ~ 최신 |
| 이석훈의 브런치카페 (`BRUNCH_CAFE`) | `FM4U000001334` | 1 ~ 1194 (종영) |
| 김이나의 별이 빛나는 밤에 (`STARNIGHT`) | `RASFM210` | 2000 ~ 최신 |

- 세 프로그램 모두 seqID 1부터 페이지가 있다. 시작 값은 너무 오래된 회차를 빼려고 정한 값이다.
- 2025-05 무렵 선곡표 표기가 바뀌었다(대문자, `영문 (한글)`, `OST / 가수`, `이름: 역할`). 두 표기가 섞여 있으므로 이 장의 규칙은 회차와 상관없이 모든 행에 똑같이 적용한다.

### 1-2. 전체 회차 크롤링 → `radio_songs.csv`

- 크롤링할 때마다 회차 목록 페이지에서 최신 회차를 다시 읽고, 프로그램마다 시작 seqID부터 최신 회차까지 모든 선곡표를 받는다.
- 컬럼: `radio, radio_name, seq, date, title, artist` (선곡표 원문 그대로)
- 재시도해도 HTTP 500을 내는 회차는 페이지 자체가 깨진 것이라 건너뛴다. 이어하기와 네트워크 에러 처리는 [`initial_crawler/README.md`](../initial_crawler/README.md)에 있다.

### 1-3. 선곡표 행 파싱

- 곡 목록은 `tbody tr`이다. 각 행에서 두 번째 `td`가 **제목**, 세 번째 `td`가 **아티스트**다.
- `td`가 1개 이하인 행(`<오프닝>`, `<초대석>` 같은 코너 이름 행)은 곡이 아니므로 건너뛴다.
- 제목이 빈 행은 건너뛴다.
- 한 회차에 같은 (제목, 아티스트)가 두 번 나오면 하나만 남긴다.
- `&#39;` 같은 HTML 엔티티는 문자로 바꾼다. 원본에 엔티티가 남은 행이 있으므로 이후 단계에서도 읽을 때 한 번 더 바꾼다.

### 1-4. 가사 없는 곡 제거

게임은 노래를 듣고 맞히는 방식이므로 **가사가 없는 음악은 지운다.** 가사가 있는 노래는 표기가 붙어 있어도 지우지 않는다. 지운 곡은 모두 `radio_songs_removed.csv`에 사유와 함께 남긴다(1-11).

| 처리 | 판단 기준 (원본 제목의 괄호·대괄호 안, 또는 `*` 뒤) | 예 |
|---|---|---|
| 삭제: 반주·연주 버전(Inst/MR) | 단어 `Inst`, `Inst.`, `Instrumental`, `MR` 또는 `반주` | `Sweety (Inst.)`, `(MR 반주곡) 처녀뱃사공` |
| 삭제: 연주 시그널·BGM | `타이틀 뮤직`, 단어 `BGM`, `시그널 뮤직` | `눈오는 밤 (Snowy Night) (MBC 새벽다방, 책다방 BGM)` |
| 삭제: 연주 시그널·BGM(표기 없이 나온 같은 곡) | 위에서 지운 곡과 같은 곡(1-5 판정)이 표기 없이 나온 행 | `눈오는 밤 (Snowy Night) \| 이호병` |
| 유지: 표기만 뗀다 | `퀴즈용`, `시그널 송`, `로고송` | `Billie Jean (퀴즈용 음악)` → `Billie Jean` |

- `MR`은 뒤에 `.`이 오면 보지 않는다(`Daft Funk (feat. Mr. Talkbox)`).
- `시그널`은 `시그널 뮤직`·`시그널 송`일 때만 본다(`처음 보는 나 (하트시그널 삽입곡)`은 드라마 이름).

### 1-5. 같은 곡 하나로 합치기 → `radio_songs_unique.csv`

1-4와 함께 처리한다.

같은 곡이 방송마다 반복되고, 표기 형식 전환 전후로 다르게 적힌다(`VENUS | SHOCKING BLUE` / `Venus | Shocking Blue`, `PENTAGON (펜타곤)` / `펜타곤 (PENTAGON)`, `좋아해 (Original Ver.)` / `좋아해`). 이런 행을 한 곡으로 합친다.

1. **제목 키**: 1-6으로 정제한 제목에서 대소문자·공백·문장부호를 무시한다. 한자·가나·키릴 문자는 남기고, 기호만 있는 제목(`%%`)은 원문을 쓴다.
2. **가수 키**: 원문 아티스트에서 나올 수 있는 이름을 모두 모은다(전체 이름, 괄호 밖·안 이름, 첫 번째 가수, `멤버 of 그룹`의 멤버). 각 이름을 제목 키와 같은 방식으로 정규화한다.
3. 제목 키가 같고 가수 키가 **하나라도 겹치면** 같은 곡이다. 예: `JAY-Z feat. ALICIA KEYS`와 `JAY-Z (Feat. Alicia Keys)`는 `jayz`가 겹친다.
4. **대표 표기**: 같은 곡의 원문 (제목, 아티스트) 중 **가장 많이 나온 표기**를 남긴다. 횟수가 같으면 최근 방송의 표기를 쓰고, `퀴즈용` 등 표기가 붙은 원문보다 붙지 않은 원문을 먼저 고른다. 대표 표기가 나온 가장 최근 행의 방송 정보를 쓴다.

컬럼: `radio, radio_name, seq, date, title, artist`

- 버전 표기는 제목 키에서 빠지므로 리믹스·리마스터·라이브 버전도 원곡과 한 곡으로 합쳐진다.

### 1-6. 제목 정제 → `title`, `subtitle`

선곡표 제목에는 피처링, 버전, 리마스터 연도 같은 부가정보가 괄호, 대괄호, `*` 뒤, ` - ` 뒤에 붙는다. 이것을 지우고 **한글 제목(`title`)**과 **부제(`subtitle`)**로 나눈다.

1. **정규화**: 백틱(`` ` ``)과 둥근 따옴표(`’`, `‘`)는 `'`로, `“`, `”`는 `"`로, 전각 괄호는 반각으로 바꾸고 연속 공백을 하나로 줄인다.
2. **`*` 이후 제거**: 신 형식은 `*` 뒤에 버전이나 설명을 적는다(`GO GENTLE *RADIO EDIT`).
3. **대괄호는 소괄호와 똑같이** 다룬다(`비상 [2021 Remaster]`).
4. **최상위 괄호 단위로 분류한다.** `(feat. 도경수(D.O.))`처럼 중첩된 괄호는 바깥 괄호 하나로 보고 내용 전체로 판단한다.
   - 내용이 부가정보(1-7)이면 버린다.
   - `원제: X`이면 `X`를 별칭으로 남긴다.
   - 그 밖의 내용은 곡의 다른 이름이므로 **별칭**으로 남긴다.
5. **맨 앞 괄호는 제목의 일부다**: `(I Can't Get No) Satisfaction`은 괄호를 뺀 `Satisfaction`을 제목으로, 괄호만 벗긴 전체를 별칭으로 둔다.
6. **괄호 밖 feat 제거**: `feat.`, `ft.`, `featuring`, `feat `(공백 앞)부터 끝까지 지운다. `Feat.그렉`처럼 점 뒤에 공백이 없어도 지운다.
7. **` - ` 뒤 부가정보 제거**: 뒤쪽이 부가정보일 때만 지운다.
8. **제목 전체가 괄호**이면(`(Nice Dream)`) 그 내용을 제목으로 쓴다.
9. **title / subtitle 정하기**: 정제한 제목과 첫 번째 별칭 중 **한글이 있는 쪽이 `title`**, 다른 쪽이 `subtitle`이다. 둘 다 한글이거나 둘 다 한글이 없으면 정제한 제목이 `title`이다. 별칭이 없으면 `subtitle`은 빈 값이다.

| 원본 제목 | `title` | `subtitle` |
|---|---|---|
| `Breath (넌 날 숨 쉬게 해)` | `넌 날 숨 쉬게 해` | `Breath` |
| `센 척 안 해 (One of Those Nights) (Feat. Crush)` | `센 척 안 해` | `One of Those Nights` |
| `十二夜 (십이야)` | `십이야` | `十二夜` |
| `Love Paint (every afternoon)` | `Love Paint` | `every afternoon` |
| `작은 것들을 위한 시 (Boy With Luv) feat. Halsey` | `작은 것들을 위한 시` | `Boy With Luv` |
| `행복하니? (Part 2) (원제: Feel So Good)` | `행복하니?` | `Feel So Good` |
| `(I Can't Get No) Satisfaction (Mono Version)` | `Satisfaction` | `I Can't Get No Satisfaction` |
| `small girl (feat. 도경수(D.O.)) (asdf)` | `small girl` | `asdf` |
| `(Nice Dream)` | `Nice Dream` | (빈 값) |
| `비상 [2021 Remaster]` | `비상` | (빈 값) |
| `GO GENTLE *RADIO EDIT` | `GO GENTLE` | (빈 값) |
| `너라서 Feat.그렉` | `너라서` | (빈 값) |
| `Let It Go (From "Frozen")` | `Let It Go` | (빈 값) |
| `주홍글씨 (Hot Play Funky Remix)` | `주홍글씨` | (빈 값) |
| `Remix - Nonstop Ivy Club Remix` | `Remix` | (빈 값) |
| `사랑해` | `사랑해` | (빈 값) |

### 1-7. 부가정보 키워드

괄호 내용, `*` 뒤, ` - ` 뒤가 부가정보인지 판단한다. 아래 중 하나라도 맞으면 부가정보다.

1. **영어 키워드가 단어로 들어 있다**(대소문자 무시, 앞뒤가 알파벳·한글이 아니어야 한다. `FT아일랜드`의 `FT`는 단어가 아니다):
   `feat, featuring, ft, prod, produced, remaster, remastered, mastered, live, inst, instrumental, mr, remix, mix, ver, version, edit, album, single, original, radio, extended, acoustic, lp, ep, explicit, clean, bonus, digital, mono, stereo, duet, narr, narration, vocal, vocals, cf, ost, theme, soundtrack, demo, intro, outro, reprise, unplugged, session, track, cover, bgm, english, japanese, korean, chinese, stage, piano, orchestra, orchestral, sped, slowed, rap, unit`
2. **흔한 영어 단어는 맨 앞에 올 때만** 본다: `with, from, part, pt, for, by, in the style of`. 그래서 `(with 아이유)`, `(From "Frozen")`, `(Part 2)`는 부가정보이고 `(Boy With Luv)`는 별칭이다.
3. **연도**가 들어 있다: `19xx`, `20xx`.
4. 내용이 ` 시`로 끝난다(시 낭송 출처: `(Merci Cherie, 김현승 시)`).
5. **한국어 키워드가 들어 있다**(부분 문자열): `영화, 드라마, 광고, 삽입곡, 원곡, 원제, 편곡, 버전, 라이브, 테마, 시그널, 타이틀, 퀴즈, 반주, 오프닝, 엔딩, 주제가, 방송, 리메이크, 리믹스, 연주, 합창, 듀엣, 보컬, 나레이션, OST`. 단 `라이브`는 `드라이브` 안에 있으면 보지 않는다(`알파드라이브원`).
6. 내용이 비어 있다.

### 1-8. 가수 정제 → `artist`, `artist_sub`

가수 한 명마다 **한글 이름(`artist`)**과 **병기된 다른 언어 이름(`artist_sub`)**을 짝으로 만든다. 병기가 없으면 같은 이름을 두 번 쓴다. 가수가 여러 명이면 이 짝을 원본 순서대로 **`artists` 배열**에 담는다(1-11). 아래 예시 표에서는 보기 쉽게 여러 명을 `, `로 이어 적었다.

1. **정규화**: 1-6과 같다. 대괄호는 소괄호로 바꾸고 맨 앞 `/`를 지운다.
2. **특별 케이스 보호**: 1-9 표의 이름을 먼저 찾아 자리표시자로 바꾼다. 이후 단계에서 나누지도 괄호를 지우지도 않는다.
3. **아티스트 없음**: 빈 값, `Various Artists`, `V.A`, `V.A.`, `OST`, `Unknown`이면 그 곡은 **삭제**한다(1-11). 아래 단계를 거친 뒤 빈 값이 되어도(`feat. 정엽`) 삭제한다.
4. **`OST / ` 접두어**를 지운다.
5. **` / `로 나뉜 조각**(앞뒤 공백이 있는 `/`만. `AC/DC`는 이름): `이름: VOCAL / 이름: PRODUCE`처럼 역할이 적혀 있으면 부른 사람(`VOCAL`, `SOLO`, `RAP`)의 조각을, 아니면 앞 조각을 쓴다.
6. **크레딧 접두어**(`원곡:`, `노래:`, `작곡:`, `작사:`, `편곡:`)와 **괄호 안 부가정보**(`(feat. …)`, `(원곡 : …)`, `(Duet With …)`)를 지운다.
7. **피처링 제거**: 괄호 밖의 `feat.`, `featuring`, `feat `, `ft.`부터 끝까지 지운다. **`ft`는 점이 붙은 `ft.`일 때만** 피처링으로 본다(`FT ISLAND`는 이름).
8. **뒤에 붙은 공연 정보**(`live`, `narr.`, `duet` 이후)를 지운다.
9. **역할 제거**: `: 역할`을 지운다. 역할은 다음 단어일 때만 인정한다(그래서 `CLASS:y`, `ZE:A`, `E:RODA`는 이름으로 남는다):
   `VOCAL, VOCALS, PRODUCE, PRODUCER, PIANO, GUITAR, BASS, SAXOPHONE, TENOR SAXOPHONE, ALTO SAXOPHONE, SAX, VIOLIN, TRUMPET, FLUGELHORN, SOLO, NARRATION, VERSE, MIX, DRUMS, CELLO, BANDONEON, TENOR, RAP, CHORUS, DIR`
10. **여러 명 나누기**: 괄호 밖에서 아래 구분자로 나눈다.
    - `,` (단 `, Jr`는 이름의 일부)
    - `&` (공백 유무 상관없음)
    - `+` (공백 유무 상관없음. `SUMIN+Zion.T`)
    - 앞뒤 공백이 있는 `and`, `x`, `with`, `vs`, `×` (대소문자 무시)
    - 한글 사이에 공백 없이 붙은 `x`/`X` (`김사월X김해원`, `도겸X승관`)
    - 나눈 뒤 조각이 `The …`, `His …`, `Her …`로 시작하거나 `Sons`, `Friends`, `Papas`, `Chorus`이면 앞 조각과 다시 붙인다. `A & The B`, `A and His Orchestra`는 한 팀 이름이기 때문이다. 단 `The Chainsmokers`, `The Fat Boys`, `The Police`, `The Edge`는 별개의 아티스트다.
11. **조각마다 이름 정하기**
    - `멤버 of 그룹` / `멤버 from 그룹`이면 멤버만 남긴다. 단, 멤버나 그룹 쪽에 한글이 있거나 그룹이 따로 활동한 아티스트일 때만 이렇게 본다. 그렇지 않으면 `KISS OF LIFE`, `5 Seconds of Summer`처럼 `of`가 든 팀 이름이다.
    - `이름 (병기)`이면 한글이 있는 쪽이 `artist`, 다른 쪽이 `artist_sub`다. 괄호 안에 `/`로 여러 이름이 있으면 첫 이름을 쓴다.
    - 괄호 안이 **여러 명의 목록**(`PDIS (조PD+윤일상)`)이면 버린다.
    - 괄호 안이 **소속 그룹**이면 버린다. 소속 그룹 판단 기준:
      1. 둘 다 한글이다(`솔라 (마마무)`).
      2. 괄호 안 이름이 다른 곡에서 자기 병기 이름과 함께 나오는데, 그 병기 이름이 지금 이름과 다르다(`부석순 (SEVENTEEN)`: `SEVENTEEN (세븐틴)`).
      3. 2~3글자 한글 이름 뒤의 영문 이름이 혼자 적힌 곡이 3곡 이상이다(`산들 (B1A4)`).
      - 이름 비교는 대소문자·공백·문장부호를 무시한다(`FT ISLAND` = `FTISLAND`).
12. **이름 보정**: `체리 필터` → `체리필터`. 같은 가수가 두 번 나오면 하나만 남긴다.

| 원본 아티스트 | `artist` | `artist_sub` |
|---|---|---|
| `혁오 오혁 x 이인우` | `혁오 오혁, 이인우` | `혁오 오혁, 이인우` |
| `박명수 X 딘딘` | `박명수, 딘딘` | `박명수, 딘딘` |
| `김사월X김해원` | `김사월, 김해원` | `김사월, 김해원` |
| `청하, COLDE (콜드)` | `청하, 콜드` | `청하, COLDE` |
| `BLOCK B (블락비)` | `블락비` | `BLOCK B` |
| `자우림 (JAURIM)` | `자우림` | `JAURIM` |
| `LOCO (로꼬), SAM KIM (샘 김)` | `로꼬, 샘 김` | `LOCO, SAM KIM` |
| `FT ISLAND (에프티 아일랜드)` | `에프티 아일랜드` | `FT ISLAND` |
| `DR. DRE feat. SNOOP DOGG` | `DR. DRE` | `DR. DRE` |
| `TAEYEON ft. Dean` | `TAEYEON` | `TAEYEON` |
| `KENNY ROGERS with DOLLY PARTON` | `KENNY ROGERS, DOLLY PARTON` | `KENNY ROGERS, DOLLY PARTON` |
| `SUMIN+Zion.T` | `SUMIN, Zion.T` | `SUMIN, Zion.T` |
| `채영 of TWICE (트와이스)` | `채영` | `채영` |
| `Young K (영케이) of DAY6 (데이식스)` | `영케이` | `Young K` |
| `KISS OF LIFE (키스오브라이프)` | `키스오브라이프` | `KISS OF LIFE` |
| `5 Seconds of Summer` | `5 Seconds of Summer` | `5 Seconds of Summer` |
| `산들 (B1A4)` | `산들` | `산들` |
| `부석순 (SEVENTEEN)` | `부석순` | `부석순` |
| `PDIS (조PD+윤일상)` | `PDIS` | `PDIS` |
| `OST / 태연` | `태연` | `태연` |
| `신용재: VOCAL / Rabbit Hit the Dragon (래빗 힛 더 드래곤): PRODUCE` | `신용재` | `신용재` |
| `MILES DAVIS: TRUMPET` | `MILES DAVIS` | `MILES DAVIS` |
| `CLASS:y (클라씨)` | `클라씨` | `CLASS:y` |
| `로이킴 (원곡 : Damien Rice)` | `로이킴` | `로이킴` |
| `산들 (B1A4) live` | `산들` | `산들` |
| `Bob Seger & The Silver Bullet Band` | `Bob Seger & The Silver Bullet Band` | `Bob Seger & The Silver Bullet Band` |
| `Mumford & Sons, Chris Stapleton` | `Mumford & Sons, Chris Stapleton` | `Mumford & Sons, Chris Stapleton` |
| `Leslie Odom, Jr` | `Leslie Odom, Jr` | `Leslie Odom, Jr` |
| `(여자)아이들` | `(여자)아이들` | `(G)I-DLE` |
| `민니 ((여자)아이들)` | `민니` | `민니` |
| `F(X) (에프엑스)` | `에프엑스` | `f(x)` |
| `TOMORROW X TOGETHER (투모로우바이투게더)` | `투모로우바이투게더` | `TOMORROW X TOGETHER` |
| `EARTH, WIND AND FIRE` | `Earth, Wind & Fire` | `Earth, Wind & Fire` |
| `CHERRY FILTER (체리 필터)` | `체리필터` | `CHERRY FILTER` |
| `Various Artists` | (삭제: 아티스트 없음) |  |

### 1-9. 특별 케이스 이름

구분자(`&`, `and`, `X`, `+`, `,`)나 괄호가 **이름의 일부**인 팀이다. 나누지도 괄호를 지우지도 않고, 아래 `artist`, `artist_sub`로 바꾼다. 데이터는 `scripts/radio/special_artists.csv`에 있고 스크립트가 그대로 읽는다.

- **찾는 방법**: 대소문자를 무시하고, 앞뒤가 글자·숫자가 아닌 위치에서 찾는다. 패턴의 ` (&|and) `는 `Belle&Sebastian`처럼 공백 없는 `&`도 허용한다.
- **후보를 뽑는 방법**: 크롤링할 때마다 다음 두 종류를 뽑아 하나씩 검토하고, 팀·듀오·유닛 이름만 표에 넣는다.
  - 구분자가 든 아티스트 문자열 중 **서로 다른 곡 3곡 이상**에 같은 조합으로 나오는 것
  - `x`/`X`가 든 아티스트 전부
  - 협업 표기(`박명수 X 딘딘`, `Future, Metro Boomin`, `송소희 & 두번째달`)는 표에 넣지 않고 나눈다.

| 패턴 | `artist` | `artist_sub` | 비고 |
|---|---|---|---|
| `\(여자\)\s*아이들` | (여자)아이들 | (G)I-DLE | 괄호가 든 이름 |
| `f\(x\)` | 에프엑스 | f(x) | 괄호가 든 이름 |
| `\(\(\( O \)\)\)` | ((( O ))) | ((( O ))) | 괄호가 든 이름 |
| `TOMORROW X TOGETHER` | 투모로우바이투게더 | TOMORROW X TOGETHER | X가 든 이름 |
| `MONSTA X` | 몬스타엑스 | MONSTA X | X가 든 이름 |
| `PRODUCE X 101` | PRODUCE X 101 | PRODUCE X 101 | X가 든 이름 |
| `Chloe X Halle` | Chloe X Halle | Chloe X Halle | X가 든 듀오 이름 |
| `ELVIS VS JXL` | Elvis vs JXL | Elvis vs JXL | vs가 든 이름 |
| `SUPER JUNIOR-D&E` | 슈퍼주니어-D&E | SUPER JUNIOR-D&E | &가 든 유닛 이름 |
| `15&` | 15& | 15& | &가 든 이름 |
| `&TEAM` | 앤팀 | &TEAM | &가 든 이름 |
| `세훈&찬열` | 세훈&찬열 | SEHUN&CHANYEOL | &가 든 유닛 이름 |
| `레드벨벳-아이린&슬기` | 레드벨벳-아이린&슬기 | Red Velvet - IRENE & SEULGI | &가 든 유닛 이름 |
| `김희철&김정모` | 김희철&김정모 | 김희철&김정모 | &가 든 유닛 이름 |
| `스컬&하하` | 스컬&하하 | SKULL&HAHA | &가 든 유닛 이름 |
| `W\s*&\s*Whale` | W&Whale | W&Whale | &가 든 이름 |
| `Simon (&\|and) Garfunkel` | Simon & Garfunkel | Simon & Garfunkel | &가 든 팀 이름 |
| `Earth,? Wind (&\|and) Fire` | Earth, Wind & Fire | Earth, Wind & Fire | 쉼표·&가 든 팀 이름 |
| `Blood,? Sweat (&\|and) Tears` | Blood, Sweat & Tears | Blood, Sweat & Tears | 쉼표·&가 든 팀 이름 |
| `Peter,? Paul (&\|and) Mary` | Peter, Paul & Mary | Peter, Paul & Mary | 쉼표·&가 든 팀 이름 |
| `Crosby,? Stills,? Nash (&\|and) Young` | Crosby, Stills, Nash & Young | Crosby, Stills, Nash & Young | 쉼표·&가 든 팀 이름 |
| `Crosby,? Stills (&\|and) Nash` | Crosby, Stills & Nash | Crosby, Stills & Nash | 쉼표·&가 든 팀 이름 |
| `Emerson,? Lake (&\|and) Palmer` | Emerson, Lake & Palmer | Emerson, Lake & Palmer | 쉼표·&가 든 팀 이름 |
| `Tyler, The Creator` | Tyler, The Creator | Tyler, The Creator | 쉼표가 든 이름 |
| `Car, the garden` | 카더가든 | Car, the garden | 쉼표가 든 이름 |
| `(Daryl )?Hall (&\|and) (John )?Oates` | Hall & Oates | Hall & Oates | &가 든 팀 이름 |
| `Mumford (&\|and) Sons` | Mumford & Sons | Mumford & Sons | &가 든 팀 이름 |
| `Years (&\|and) Years` | Years & Years | Years & Years | &가 든 팀 이름 |
| `Florence \+ The Machine` | Florence + The Machine | Florence + The Machine | +가 든 팀 이름 |
| `Dan \+ Shay` | Dan + Shay | Dan + Shay | +가 든 팀 이름 |
| `Angus (&\|and) Julia Stone` | Angus & Julia Stone | Angus & Julia Stone | &가 든 팀 이름 |
| `Sonny (&\|and) Cher` | Sonny & Cher | Sonny & Cher | &가 든 팀 이름 |
| `Of Monsters (&\|and) Men` | Of Monsters and Men | Of Monsters and Men | and가 든 팀 이름 |
| `The Bird (&\|and) The Bee` | The Bird and the Bee | The Bird and the Bee | and가 든 팀 이름 |
| `Iron (&\|and) Wine` | Iron & Wine | Iron & Wine | &가 든 팀 이름 |
| `She (&\|and) Him` | She & Him | She & Him | &가 든 팀 이름 |
| `Captain (&\|and) Tennille` | Captain & Tennille | Captain & Tennille | &가 든 팀 이름 |
| `Tony Orlando (&\|and) Dawn` | Tony Orlando & Dawn | Tony Orlando & Dawn | &가 든 팀 이름 |
| `Belle (&\|and) Sebastian` | Belle & Sebastian | Belle & Sebastian | &가 든 팀 이름 |
| `K-Ci (&\|and) JoJo` | K-Ci & JoJo | K-Ci & JoJo | &가 든 팀 이름 |
| `Sergio Mendes (&\|and) Brasil '66` | Sergio Mendes & Brasil '66 | Sergio Mendes & Brasil '66 | &가 든 팀 이름 |
| `Neil Young (&\|and) Crazy Horse` | Neil Young & Crazy Horse | Neil Young & Crazy Horse | &가 든 팀 이름 |
| `Ike (&\|and) Tina Turner` | Ike & Tina Turner | Ike & Tina Turner | &가 든 팀 이름 |
| `Jon (&\|and) Vangelis` | Jon & Vangelis | Jon & Vangelis | &가 든 팀 이름 |
| `Tegan (&\|and) Sara` | Tegan and Sara | Tegan and Sara | and가 든 팀 이름 |
| `Sam (&\|and) Dave` | Sam & Dave | Sam & Dave | &가 든 팀 이름 |
| `Macklemore (&\|and) Ryan Lewis` | Macklemore & Ryan Lewis | Macklemore & Ryan Lewis | &가 든 팀 이름 |
| `Paul McCartney (&\|and) Wings` | Paul McCartney & Wings | Paul McCartney & Wings | &가 든 팀 이름 |
| `Tones (&\|and) I` | Tones And I | Tones And I | and가 든 이름 |
| `You\+Me` | You+Me | You+Me | +가 든 듀오 이름 |
| `MARIO&` | 마리오 앤드 | MARIO& | &가 든 이름 |
| `H7\s*美人(\s*\(미인\))?` | H7 미인 | H7 美人 | 괄호·한자가 든 이름 |
| `Gym (&\|and) Swim` | Gym and Swim | Gym and Swim | and가 든 팀 이름 |
| `Al Bano (&\|and) Romina Power` | Al Bano & Romina Power | Al Bano & Romina Power | &가 든 듀오 이름 |

### 1-11. 결과 파일

**`radio_songs_final.json`** — 게임에 쓰는 최종 노래 목록. 곡 객체의 배열이다.

| 컬럼 | 내용 |
|---|---|
| `radio`, `radio_name`, `seq`, `date` | 대표 표기가 나온 가장 최근 방송 |
| `title`, `subtitle` | 1-6 |
| `artists` | 1-8. `[{"artist": 한글 이름, "artist_sub": 병기 이름}, …]`. 원본에 적힌 순서대로 담는다 |
| `raw_title`, `raw_artist` | `radio_songs_unique.csv`의 원문 |

예:

```json
{
  "radio": "MUSIC_PARTY", "radio_name": "정오의 희망곡 김신영입니다", "seq": 3437, "date": "2018-11-06",
  "title": "하루가 가고 또 하루가 오면", "subtitle": "",
  "artists": [
    {"artist": "혁오 오혁", "artist_sub": "혁오 오혁"},
    {"artist": "이인우", "artist_sub": "이인우"}
  ],
  "raw_title": "하루가 가고 또 하루가 오면 (feat. Jay Marie)", "raw_artist": "혁오 오혁 x 이인우"
}
```

**`radio_songs_removed.csv`** — 삭제한 곡 목록

컬럼: `stage, reason, radio, radio_name, seq, date, title, artist, count`. 원문 (제목, 아티스트)마다 한 행이고, `count`는 삭제된 원본 행 수다. 중복으로 합쳐진 행은 삭제가 아니므로 넣지 않는다.

| stage | reason |
|---|---|
| unique | 반주·연주 버전(Inst/MR) |
| unique | 연주 시그널·BGM |
| unique | 연주 시그널·BGM(표기 없이 나온 같은 곡) |
| final | 아티스트 없음 |

**`radio_songs_unresolved.csv`** — 규칙으로 처리하지 못한 곡(final에서 뺀다).

컬럼: `radio, radio_name, seq, date, title, subtitle, artist, artist_sub, raw_title, raw_artist, reason`. 정제 결과도 참고용으로 채워 둔다. 사람이 표로 검토하는 파일이라 CSV로 두고, 가수 여러 명은 `artist`, `artist_sub` 칸에 `, `로 이어 적는다. 한 곡이 여러 이유에 걸리면 `; `로 모두 적는다. 사람이 확인해 고치거나 특별 케이스 표에 추가한 뒤 다시 실행한다.

| reason | 판정 기준 | 예 |
|---|---|---|
| 여러 곡을 이어 붙인 제목 | 정제한 제목에 `/`가 있고 양쪽에 글자가 있다(`24/7`, `1/2` 같은 숫자는 제외) | `그녀와의 이별/혼자한 사랑/진실과 테크닉`, `instinct/본능` |
| 클래식 작품명 | `작곡가:`로 시작하거나 `Op.`, `BWV`, `RV`, `K.`, `Hob.` 작품번호가 있다 | `Tchaikovsky : Concerto For Violin In D Major Op.35 - III. …` |
| 한글·영문이 없는 제목 | 제목이 한자·가나·키릴 문자뿐이어서 한글 `title`을 정할 수 없다 | `藏愛`, `幻想童話 (Secret Story of the Swan)` |
| 슬래시(/)로 이어진 아티스트 | 역할 표기 없이 ` / `로 이어져 그룹/멤버인지 협업인지 알 수 없다 | `XLOV (엑스러브) / 현, 하루`, `FRANK SINATRA / ANTONIO CARLOS JOBIM` |
| 메들리 | 원본 제목이 `*`로 시작한다 | `*MEDLEY / 1. AQUARIUS / 2. …` |
| 대시(-)로 이어진 아티스트 | 아티스트에 ` - `가 있다 | `소녀시대 - 태티서`, `G - Dragon` |
| 제목 없음 | 제목 전체가 부가정보다 | `(Live In Rome / 1990)` |

- `instinct/본능`처럼 `/`로 제목과 번역을 이은 한 곡도 규칙으로는 메들리와 구별할 수 없어 여기에 들어간다.

### 1-12. 구현할 때 주의할 점

- **키워드와 구분자는 단어 단위로 비교한다.** 부분 문자열로 비교하면 `ver`가 `every`에, `ft`가 `FT ISLAND`·`TAYLOR SWIFT`에, `and`가 `Ariana Grande`에 걸린다. 한국어 키워드만 부분 문자열로 비교한다.
- **구분자는 괄호 밖에서만 찾는다.** `드렁큰 타이거 (feat. 윤미래, Bizzy)`를 `,`로 먼저 나누면 괄호가 깨진다.
- **괄호는 최상위 단위로 분류한다.** 가장 안쪽 괄호부터 처리하면 `(feat. 도경수(D.O.))`의 `D.O.`가 별칭으로 뽑힌다.
- **특별 케이스는 가장 먼저 보호한다.** 다른 규칙이 먼저 돌면 `(여자)아이들`의 괄호가 지워지고 `Earth, Wind & Fire`가 세 명으로 나뉜다.
- **소속 그룹 판단(1-8의 11번)은 데이터 전체를 본다.** 한 행만 보고는 `자우림 (JAURIM)`(병기)과 `산들 (B1A4)`(소속)을 구별할 수 없다. 그래서 `make_final.py`는 먼저 unique 파일 전체에서 혼자 적힌 아티스트 이름과 병기 이름을 모은다.
