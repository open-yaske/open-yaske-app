# データベース設計

OpenYASKE のユーザーデータは「学校の情報だけ」を保存する。
個人の予定と TODO は端末のカレンダー / リマインダーに任せ、アプリには持たない。

- 日付は `YYYY-MM-DD` の TEXT。epoch millis は監査・同期用の時刻だけに使う
- ID は UUID の TEXT
- 削除は論理削除（`deleted_at_ms`）。端末同期の墓標として使う
- 文字列の列挙は CHECK で制限する
- 期間・時限・状態はアプリ側でも検証するが、DB でも破れないようにする

## 全体像

```text
terms ─┬─< courses ─┬─< course_sessions ─< attendance_records
       │            ├─< course_tasks
       │            ├─< course_grades
       │            └─< external_sync_links (polymorphic)
       └─< school_events
gpa_scenarios ─< gpa_scenario_overrides >─ courses
app_meta / app_settings
scan_templates ─< scanned_documents
```

## DDL

SQLite（SQLDelight）を想定した形。

```sql
CREATE TABLE app_meta (
    id              INTEGER PRIMARY KEY CHECK (id = 1),
    schema_version  INTEGER NOT NULL,
    updated_at_ms   INTEGER NOT NULL
);

CREATE TABLE terms (
    id          TEXT PRIMARY KEY,
    year        INTEGER NOT NULL CHECK (year BETWEEN 1 AND 9999),
    kind        TEXT NOT NULL CHECK (kind IN ('semester','full_year','intensive','quarter','other')),
    season      TEXT CHECK (season IN ('spring','fall')),
    start_date  TEXT NOT NULL CHECK (start_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    end_date    TEXT NOT NULL CHECK (end_date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    label       TEXT NOT NULL DEFAULT '',
    CHECK (start_date <= end_date)
);
CREATE UNIQUE INDEX ux_terms_year_season ON terms(year, season) WHERE season IS NOT NULL;

CREATE TABLE courses (
    id                TEXT PRIMARY KEY,
    title             TEXT NOT NULL,
    code              TEXT,
    instructor        TEXT,
    room              TEXT,
    url               TEXT,
    memo              TEXT,
    course_type       TEXT NOT NULL CHECK (course_type IN ('required','elective','free','teaching')),
    kind              TEXT NOT NULL CHECK (kind IN ('regular','extra','intensive','partner','user')),
    source            TEXT NOT NULL CHECK (source IN ('public_syllabus','user','partner')),
    institution       TEXT,
    credits           INTEGER NOT NULL DEFAULT 0 CHECK (credits BETWEEN 0 AND 30),
    credit_system     TEXT NOT NULL CHECK (credit_system IN ('uec','ects','other')),
    counts_toward_gpa INTEGER NOT NULL DEFAULT 1 CHECK (counts_toward_gpa IN (0,1)),
    weekday           INTEGER CHECK (weekday BETWEEN 1 AND 7),
    period            INTEGER CHECK (period BETWEEN 1 AND 7),
    period_count      INTEGER NOT NULL DEFAULT 1 CHECK (period_count BETWEEN 1 AND 3),
    color             TEXT NOT NULL,
    syllabus_id       TEXT,
    term_id           TEXT REFERENCES terms(id) ON DELETE SET NULL,
    academic_year_id  TEXT,
    created_at_ms     INTEGER NOT NULL,
    updated_at_ms     INTEGER NOT NULL,
    deleted_at_ms     INTEGER,
    CHECK ((weekday IS NULL) = (period IS NULL))
);
CREATE INDEX ix_courses_term ON courses(term_id) WHERE deleted_at_ms IS NULL;
CREATE INDEX ix_courses_code ON courses(code) WHERE code IS NOT NULL;

CREATE TABLE course_sessions (
    id                TEXT PRIMARY KEY,
    course_id         TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    date              TEXT NOT NULL CHECK (date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    start_period      INTEGER CHECK (start_period BETWEEN 1 AND 7),
    period_count      INTEGER NOT NULL DEFAULT 1 CHECK (period_count BETWEEN 1 AND 3),
    start_time        TEXT CHECK (start_time IS NULL OR start_time GLOB '[0-2][0-9]:[0-5][0-9]'),
    end_time          TEXT CHECK (end_time IS NULL OR end_time GLOB '[0-2][0-9]:[0-5][0-9]'),
    room              TEXT,
    kind              TEXT NOT NULL CHECK (kind IN ('regular','intensive','makeup','exam','extra','partner')),
    status            TEXT NOT NULL CHECK (status IN ('scheduled','cancelled','moved')),
    note              TEXT,
    replaces_session_id TEXT REFERENCES course_sessions(id) ON DELETE SET NULL,
    created_at_ms     INTEGER NOT NULL,
    updated_at_ms     INTEGER NOT NULL,
    deleted_at_ms     INTEGER,
    CHECK (start_period IS NOT NULL OR start_time IS NOT NULL)
);
CREATE INDEX ix_sessions_date ON course_sessions(date) WHERE deleted_at_ms IS NULL;
CREATE INDEX ix_sessions_course_date ON course_sessions(course_id, date) WHERE deleted_at_ms IS NULL;
CREATE UNIQUE INDEX ux_sessions_slot ON course_sessions(
    course_id, date, COALESCE(start_period, 0), COALESCE(start_time, ''), kind
) WHERE deleted_at_ms IS NULL;

CREATE TABLE attendance_records (
    id              TEXT PRIMARY KEY,
    course_id       TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    session_id      TEXT REFERENCES course_sessions(id) ON DELETE SET NULL,
    date            TEXT NOT NULL CHECK (date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    status          TEXT NOT NULL CHECK (status IN ('present','absent','late','excused','cancelled')),
    recorded_at_ms  INTEGER NOT NULL,
    deleted_at_ms   INTEGER
);
CREATE UNIQUE INDEX ux_attendance_session ON attendance_records(session_id) WHERE session_id IS NOT NULL;
CREATE UNIQUE INDEX ux_attendance_day ON attendance_records(course_id, date) WHERE session_id IS NULL;
CREATE INDEX ix_attendance_course ON attendance_records(course_id) WHERE deleted_at_ms IS NULL;

CREATE TABLE school_events (
    id              TEXT PRIMARY KEY,
    title           TEXT NOT NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('academic','holiday','exam','makeup','cancelled_class','other')),
    date            TEXT NOT NULL CHECK (date GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'),
    start_time      TEXT CHECK (start_time IS NULL OR start_time GLOB '[0-2][0-9]:[0-5][0-9]'),
    end_time        TEXT CHECK (end_time IS NULL OR end_time GLOB '[0-2][0-9]:[0-5][0-9]'),
    all_day         INTEGER NOT NULL DEFAULT 1 CHECK (all_day IN (0,1)),
    course_id       TEXT REFERENCES courses(id) ON DELETE CASCADE,
    session_id      TEXT REFERENCES course_sessions(id) ON DELETE CASCADE,
    note            TEXT,
    source          TEXT NOT NULL CHECK (source IN ('public','user','derived')),
    created_at_ms   INTEGER NOT NULL,
    updated_at_ms   INTEGER NOT NULL,
    deleted_at_ms   INTEGER
);
CREATE INDEX ix_school_events_date ON school_events(date) WHERE deleted_at_ms IS NULL;

CREATE TABLE course_tasks (
    id              TEXT PRIMARY KEY,
    course_id       TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    session_id      TEXT REFERENCES course_sessions(id) ON DELETE SET NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('assignment','report','quiz','exam','prep','other')),
    title           TEXT NOT NULL,
    due_date        TEXT CHECK (due_date IS NULL OR date(due_date) = due_date),
    due_time        TEXT CHECK (due_time IS NULL OR due_time GLOB '[0-2][0-9]:[0-5][0-9]'),
    status          TEXT NOT NULL CHECK (status IN ('open','done','submitted','cancelled')),
    priority        TEXT CHECK (priority IN ('low','medium','high')),
    notes           TEXT,
    url             TEXT,
    created_at_ms   INTEGER NOT NULL,
    updated_at_ms   INTEGER NOT NULL,
    deleted_at_ms   INTEGER
);
CREATE INDEX ix_tasks_due ON course_tasks(due_date, status) WHERE deleted_at_ms IS NULL;
CREATE INDEX ix_tasks_course ON course_tasks(course_id) WHERE deleted_at_ms IS NULL;

CREATE TABLE course_grades (
    course_id        TEXT PRIMARY KEY REFERENCES courses(id) ON DELETE CASCADE,
    state            TEXT NOT NULL CHECK (state IN ('not_graded','graded','pass_fail','excluded','transfer','in_progress')),
    grade            TEXT CHECK (grade IN ('S','A','B','C','F')),
    credits_earned   INTEGER CHECK (credits_earned BETWEEN 0 AND 30),
    counts_toward_gpa INTEGER NOT NULL DEFAULT 1 CHECK (counts_toward_gpa IN (0,1)),
    note             TEXT,
    updated_at_ms    INTEGER NOT NULL,
    CHECK (state <> 'graded' OR grade IS NOT NULL)
);

CREATE TABLE gpa_scenarios (
    id              TEXT PRIMARY KEY,
    name            TEXT NOT NULL,
    created_at_ms   INTEGER NOT NULL
);

CREATE TABLE gpa_scenario_overrides (
    scenario_id TEXT NOT NULL REFERENCES gpa_scenarios(id) ON DELETE CASCADE,
    course_id   TEXT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    grade       TEXT NOT NULL CHECK (grade IN ('S','A','B','C','F')),
    PRIMARY KEY (scenario_id, course_id)
);

CREATE TABLE external_sync_links (
    id                  TEXT PRIMARY KEY,
    local_type          TEXT NOT NULL CHECK (local_type IN ('session','event','task')),
    local_id            TEXT NOT NULL,
    provider            TEXT NOT NULL CHECK (provider IN ('calendar','reminders')),
    external_id         TEXT NOT NULL,
    container_id        TEXT,
    direction           TEXT NOT NULL CHECK (direction IN ('to_device','from_device','both')),
    last_synced_at_ms   INTEGER NOT NULL,
    deleted_at_ms       INTEGER
);
CREATE UNIQUE INDEX ux_sync_local ON external_sync_links(provider, local_type, local_id);
CREATE UNIQUE INDEX ux_sync_external ON external_sync_links(provider, external_id);

CREATE TABLE app_settings (
    id              INTEGER PRIMARY KEY CHECK (id = 1),
    schema_version  INTEGER NOT NULL,
    json            TEXT NOT NULL,
    updated_at_ms   INTEGER NOT NULL
);

CREATE TABLE scan_templates (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    pattern     TEXT NOT NULL,
    is_default  INTEGER NOT NULL DEFAULT 0 CHECK (is_default IN (0,1)),
    created_at_ms INTEGER NOT NULL
);

CREATE TABLE scanned_documents (
    id            TEXT PRIMARY KEY,
    course_id     TEXT REFERENCES courses(id) ON DELETE SET NULL,
    session_id    TEXT REFERENCES course_sessions(id) ON DELETE SET NULL,
    file_path     TEXT NOT NULL,
    page_count    INTEGER NOT NULL DEFAULT 1,
    size_bytes    INTEGER NOT NULL DEFAULT 0,
    template_id   TEXT REFERENCES scan_templates(id) ON DELETE SET NULL,
    created_at_ms INTEGER NOT NULL
);
```

## 実装

`kmp/shared/src/commonMain/sqldelight/org/openyaske/core/db/` に SQLDelight 2.4.1 で定義している。

- 親テーブル（学期・科目・授業回・シナリオ・テンプレート）は `INSERT OR IGNORE` と `UPDATE` を同一トランザクションで実行する。
  SQLite の `REPLACE` は行を消して入れ直すため、外部キーの `ON DELETE CASCADE` で子行まで消える。
- 子を持たないテーブル（出席・学事・課題・成績・同期リンク・設定など）は `INSERT OR REPLACE` で upsert する。
- ドライバは JVM / Android / iOS 用を用意した。JVM はインメモリとファイル、Android は `AndroidSqliteDriver`、iOS は `NativeSqliteDriver`。
- Wasm は SQLDelight の web worker driver が非同期のため、UI 接続時に追加する。それまでは共通の `DatabaseRepository` をメモリ実装で差し替えられるようにする。
- 日付・時刻の CHECK は形式だけを見る。実在する日付かどうかは `UserDataCodec` と入力検証で確認する。

制約の検証は `DatabaseSchemaTest`、モデルとの往復は `DatabaseRepositoryTest` で行っている。

## 設計の理由

### 授業回を実体として持つ

週次パターンだけだと、休講・補講・集中講義・連携大学の日付変更を表現できない。
`course_sessions` を実体として持つことで、出席・通知・アジェンダ・ウィジェットが同じ行を参照できる。

通常科目は学期開始日から `SessionGenerator` で生成し、休日と上書きを適用する。
集中講義と連携大学は最初から日付指定で追加する。

### 出席は授業回に紐づける

`attendance_records.session_id` を基本にし、授業回を持たない時間割外科目だけ
`session_id IS NULL` の日付記録にする。部分ユニークインデックスで二重記録を防ぐ。

### 空きコマと時間割外を同じ表で扱う

`courses.weekday` と `period` を nullable にし、`kind` で通常・時間割外・集中・連携・ユーザー定義を区別する。
`CHECK ((weekday IS NULL) = (period IS NULL))` で片方だけ入る状態を防ぐ。

### 成績は科目に 1 行

`course_grades.course_id` を主キーにし、状態と評価を分ける。
未確定、合否、除外、認定を `state` で表現し、GPA の分母に入れるかは
`counts_toward_gpa` と科目側の設定の両方で判断する。

### 同期は外部 ID で対応付ける

`external_sync_links` に端末側の ID を保存する。`(provider, local_type, local_id)` と
`(provider, external_id)` の両方にユニーク制約を置き、二重登録と取り違えを防ぐ。

### 公開データはユーザー DB に入れない

シラバスと検索索引はビルド時に生成し、読み取り専用で同梱する。
ユーザー DB に入れるとバックアップと QR 転送が肥大化する。
検索はアプリ内の索引、または SQLite FTS5 を使う場合でも別データベースにする。

### 設定だけ JSON

設定は列が多く、画面ごとに部分更新するため、`app_settings` に
スキーマバージョン付きの JSON を 1 行だけ持つ。検証は
`settingsValidation` 相当を KMP 側で行う。検索対象ではないので列に分解しない。

## 削除と移行

- 論理削除した行は 30 日後に物理削除する。物理削除は外部同期リンクの削除が成功したものだけ
- `courses` を物理削除すると、授業回・課題・成績・出席は CASCADE で消える
- スキーマ変更は `app_meta.schema_version` と `UserData.schemaVersion` の両方で管理する
- v1（Svelte 版）からの移行では、通常科目の週次パターンから現在学期の授業回を生成する
- 旧 `gpa_entries` は読み取り専用で残し、`course_grades` へ移行できるようにする

## 主なクエリ

```text
今日のアジェンダ:
  course_sessions + school_events + course_tasks
  WHERE date = ? AND deleted_at_ms IS NULL
  ORDER BY time, kind

出席率:
  attendance_records WHERE course_id = ? AND deleted_at_ms IS NULL
  重複は session_id（なければ course_id + date）で 1 件に統合

GPA:
  course_grades JOIN courses
  WHERE state = 'graded' AND counts_toward_gpa = 1
  SUM(credits * point) / SUM(credits)
```

## やらないこと

- 個人の予定・TODO の保存
- サーバー同期、アカウント、テレメトリ
- お知らせ・レビュー・求人・提出のテーブル
- 画像や添付の DB 保存（ファイルパスだけ持つ）
