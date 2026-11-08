# IruuTreeTool

**日本語** | [English](#english)

Minecraft (Paper) 用の、木を作るツールです。幹・枝・根・葉を、部位ごとに選んだブロックで 1 本ずつ作ります。アルゴリズムを選び、数字を動かし、
あなたにだけ見える**プレビュー**で確かめてから、世界に置きます。設定は名前を付けて保存・読み込みでき、見本の木が 28 本ついています。

> **ゲーム内の動作は未確認です。** 木を作る部分 (アルゴリズム、設定の保存と読み込み、見本の木) は、単体テストで確かめています (`./gradlew build`)。
> サーバーに入れて、メニューの操作、プレビューの表示、WorldEdit / FAWE との連携 (`//undo`)、配置ツールなどを実際に動かして確かめたことは、まだありません。
> 動かない所があれば、Issue で教えてください。

もとは、RPG サーバー向けのプラグイン iruuRPG の中の「プロシージャルツール」(木のところだけ) です。単独で動くように取り出しました。

## 必要なもの

- **Paper 1.20.4** (`api-version: 1.20`。1.20.4 に対してビルドしています。ほかのバージョンは試していません)。Spigot 単体は対象外です (メニューの題名などに Paper の API を使っています)。
- Java 17。
- 任意: **FastAsyncWorldEdit** または **WorldEdit**。あれば、置いた木を `//undo` / `//redo` で戻せます (木 1 本が 1 回の編集になります)。なければ、木は直接置かれ、戻せません (起動時に警告が出ます)。

## インストール

1. GitHub の Releases から `IruuTreeTool-1.0.0.jar` をダウンロードして、サーバーの `plugins/` に入れます。
2. サーバーを起動します。`plugins/IruuTreeTool/` ができ、`config.yml` と `trees/` (見本の木) が入ります。
3. 使う人に権限 `iruutreetool.use` を渡します (初期値は OP だけ)。

見本の木は、起動のときに「まだ入れたことのないもの」だけ `trees/` に入ります。消したり書き換えたりしたものは戻しません (入れた名前は `trees/.samples` に記録されます)。

## コマンドと権限

コマンドは `/treetool` (別名 `/ptree` `/ptool` `/proceduraltool`)。プレイヤーだけが使えます。権限は **`iruutreetool.use`** (初期値 `op`) の 1 つで、コマンド、木のツール、配置ツールすべてに必要です。

| コマンド | 内容 |
|---|---|
| `/treetool` または `give` | 木のツール (オークの苗木) を受け取る |
| `/treetool off` | 木のツールを片づける |
| `/treetool menu` | 設定画面を開く (先にツールで場所を決めておく) |
| `/treetool preview` | プレビューのオン / オフ |
| `/treetool generate` | 木を置く (画面の「生成する」と同じ) |
| `/treetool undo` / `redo` | `//undo` / `//redo` と同じ (WorldEdit / FAWE があるとき) |
| `/treetool set <項目> <値>` | 数字を決める。項目: `algorithm`、`seed`、各ページの数字 (`/treetool info` で一覧と範囲) |
| `/treetool info` | いまの設定と、数字の一覧 |
| `/treetool block <部位> <ブロック名\|hand\|reset>` | 部位のブロックを選ぶ (`hand` は手に持っているブロック)。`block leaf a,b,c` は葉のグラデーション (下 → 上)、`block hang a,b` は装飾の木の垂れる房。状態つきも可: `block branch oak_log[axis=x]` |
| `/treetool save <名前>` | いまの設定を保存 (名前は英小文字・数字・`-`・`_` の 32 文字まで) |
| `/treetool load <名前>` / `list` / `delete <名前>` | 読み込み / 一覧 / 削除 (削除は `trees/trash/` へ移すだけ) |
| `/treetool presets` | プリセットの一覧画面 (クリックで読み込み、Shift+クリックで、確認の画面のあとで削除) |
| `/treetool scatter` | 配置ツールのプリセットを選ぶ画面 |
| `/treetool build <設定名> [x y z] [seed]` | 保存した設定を、その場所に置く (`~` は自分の位置。座標なしなら見ているブロックの上、見えなければ立っている所) |

## 使い方

1. `/treetool` で木のツールを受け取る。
2. **ブロックを右クリック** すると、そこに立てる木の設定画面が開きます (木は、クリックしたブロックの真上に立ちます)。遠くのブロックも、見て (空中を) 右クリックすれば届きます (`treetool.reach`、初期値 96 ブロック)。左クリックは、場所を変えずに設定画面を開きます (まだ場所がなければ、右クリックと同じに場所を決めます)。Shift+空中を右クリックすると、前回の場所の設定画面です。
3. 設定画面のページ:
   - **基本**: 置く場所、アルゴリズム、シード、プリセットの読み込み、設定のリセット、そして**部位ごとのブロック** (部位のアイコンを左クリック → 自分のインベントリのブロックをクリックで選ぶ。右クリックで初期値に戻す)。
   - **幹・根 / 枝 / 葉**: アルゴリズムごとの数字 (左クリックで増やす、右クリックで減らす、Shift で大きく動かす)。「葉」のページでは、葉のブロックを**下から上へ最大 6 段のグラデーション**にできます。
   - **陰影** (装飾の木だけ): 色の合わせ方、環境光・曲率・光源。
   - 下の段: 閉じる / **プレビュー** / **生成する** / `//undo` / `//redo`。
4. **プレビュー** をオンにすると、選んだブロックのままの試し置きが、あなたにだけ見えます (ほかの人には見えず、世界は変わりません)。シードが固定され、設定を変えてから 0.75 秒後に更新されます (`preview-delay-ticks`)。
5. 気に入ったら「生成する」。空気と草花だけを置き換えます (地形は壊しません。`replace-solid: true` で変えられます)。置いた木は `//undo` で戻せます。

### アルゴリズム

| 名前 | 内容 |
|---|---|
| `canopy` (樹冠) | 枝が同じ規則で分かれ続けるフラクタル。枝先に葉の塊、根元に根。樹冠を上へ重ねられる。柳のつる、Beta 1.7.3 風の葉 (丸太から遠い葉を消す) |
| `tiered` (段の枝) | 細る幹に、段ごとに枝 (上ほど短く上向き)。てっぺんに葉の塊、根元に根。ねじれ・編み込みの幹・雪・柳のつる |
| `palm` (ヤシ) | S 字の幹 (二股にもなる)、てっぺんに葉の層。支柱の根 |
| `ornate` (装飾の木、初期値) | 骨組み (フラクタルの枝分かれ) をパイプモデルの太さで肉付けし、太い所は丸太・階段・ハーフ、細い所は壁・フェンス・ハーフ・トラップドア・フェンスゲートを、すき間なくつなぐ。樹皮の装飾、陰影、垂れる房。様式: オーク・盆栽・トウヒ・白樺・柳・桜・アカシア。根は地面に沿わせられる (置く前に地面の高さを読む) |
| `thin` (細い木) | 若木が伸びるように 1 ブロックずつ育てる。根元から離れるほど 丸太 → 壁・ハーフ → フェンス と細くなる枝、階段の継ぎ目、重ね塗りの葉、柳のつる |
| `silhouette` (樹形) | 丸・しだれ・針葉樹・流れる幹・傘の形を骨組みで描き、細かく量って丸太・階段・ハーフ・壁・フェンスにする。葉は雲のような塊に陰影を付け、下から房が垂れる |

アルゴリズムを切り替えても、ほかのアルゴリズムの数字は残ります。

### プリセットと配置ツール

- `/treetool save <名前>` で、いまの設定 (アルゴリズム、数字、シード、選んだブロック) が `plugins/IruuTreeTool/trees/<名前>.yml` に保存されます。画面の「読み込み」(または `/treetool presets`) で一覧が開き、クリックで読み込みます。保存は、誤クリックで上書きしないよう、画面からはできません (コマンドだけ)。
- **配置ツール** (白樺の苗木): 一覧の「配置ツールのプリセットを選ぶ」(または `/treetool scatter`) で、プリセットを左クリック (重み +1) / 右クリック (重み -1) で選び、「配置ツールを受け取る」。配置ツールを右クリックすると、選んだプリセットのどれかが、重みの割合で、クリックしたブロックの上に**ランダムなシード**で置かれます。左クリックで選び直し。これも `//undo` で 1 本ずつ戻せます。
- 保存した `.yml` は手で書き換えたり、ほかのサーバーへ渡したりできます。数字は読み込むとき範囲の中に収められます。`kind:` が `tree` 以外 (iruuRPG 本体の岩や家など) のファイルは、読み込まず、一覧にも出さず、そのまま残します。

### 見本の木 (28 本)

- 樹冠: `canopy_oak` `canopy_willow` `canopy_tiers` `canopy_beta_oak` `canopy_cherry`
- 段の枝: `tiered_spruce` `tiered_snowy_pine` `tiered_braided` `tiered_weeping`
- ヤシ: `palm` `palm_twin` `palm_mangrove`
- 細い木: `thin_birch` `thin_birch_clump` `thin_birch_split` `thin_willow`
- 樹形: `silhouette_round` `silhouette_weeping` `silhouette_conifer` `silhouette_swoop` `silhouette_dome`
- 装飾の木: `ornate_fantasy` `ornate_bonsai` `ornate_spruce` `ornate_birch` `ornate_willow` `ornate_cherry` `ornate_acacia`

## 設定 (`plugins/IruuTreeTool/config.yml`)

`treetool:` の下に、次のキーがあります (変えたら、再起動または `/reload`)。

| キー | 初期値 | 内容 |
|---|---|---|
| `max-blocks` | 60000 | 1 本の木のブロック数の上限 (超えたら置かない) |
| `blocks-per-tick` | 4000 | 1 tick に調べて置くブロック数 |
| `preview-delay-ticks` | 15 | 設定を最後に変えてから、プレビューを更新するまでの tick |
| `preview-blocks-per-tick` | 3000 | プレビューで 1 tick に送るブロック数 |
| `preview-max-blocks` | 40000 | プレビューする木のブロック数の上限 (超えたら見せない) |
| `reach` | 96 | ツールが届く距離 (8〜256 ブロック) |
| `replace-solid` | false | true で、地形など固いブロックも置き換える (岩盤とバリアは別) |
| `materials.<部位>` | (各部位の既定ブロック) | 部位ごとのブロックの初期値。画面かコマンドで選んだものが優先 |

## ビルド

```
./gradlew build
```

Java 17 の JDK が必要です (Gradle は wrapper が取得します)。`build/libs/IruuTreeTool-1.0.0.jar` ができ、テストも走ります。

## 表示の言語について

メニューやメッセージの文章は日本語です (翻訳は、まだしていません)。

## 由来について

- `canopy` (樹冠)・`tiered` (段の枝)・`palm` (ヤシ) は、作者 (iruuRPG) 自身の Lua の木のスクリプト 3 本を、このツールのアルゴリズムとして移したものです (数字の名前・範囲・初期値はスクリプトのまま)。
- `silhouette` (樹形) のアルゴリズム (`silhouette/` と `SilhouetteTree`) は、作者自身の木のジェネレーター mc_tree_gen を、このツールのアルゴリズムとして移したものです。
- `thin` (細い木) は、木の作り方の動画と白樺の作例を参考にしています。

## ライセンス

[MIT License](LICENSE) — Copyright (c) 2026 iruuRPG

---

<a id="english"></a>

# IruuTreeTool (English)

A tree-building tool for Minecraft (Paper). It grows a tree block by block — trunk, branches, roots and leaves — in the blocks you choose for each part. Pick an
algorithm, move the numbers, look at it in a **preview** that only you can see, then put it in the world. Settings can be saved and loaded by name, and 28 sample trees come with it.

> **Not tested in game.** The tree generation (the algorithms, saving and loading of settings, the sample trees) is covered by unit tests (`./gradlew build`).
> I have not run the plugin on a server: the menu clicks, the preview display, the WorldEdit / FAWE integration (`//undo`) and the placing tool are unverified.
> If something does not work, please open an issue.

It was taken out of the "procedural tool" of iruuRPG, an RPG server plugin (the tree part only), to work on its own.

## Requirements

- **Paper 1.20.4** (`api-version: 1.20`; built against 1.20.4, other versions are untested). Plain Spigot is not a target (Paper's API is used, e.g. for menu titles).
- Java 17.
- Optional: **FastAsyncWorldEdit** or **WorldEdit**. With one of them a placed tree can be taken back with `//undo` / `//redo` (one tree is one edit). Without, trees are placed directly and cannot be undone (a warning is logged at startup).

## Installation

1. Download `IruuTreeTool-1.0.0.jar` from the Releases page of this repository and put it in the server's `plugins/` folder.
2. Start the server. `plugins/IruuTreeTool/` is created with `config.yml` and `trees/` (the samples).
3. Give the permission `iruutreetool.use` to whoever should use it (OPs have it by default).

Sample trees are written to `trees/` once each: only the ones that were never written before (their names are kept in `trees/.samples`), so a sample you deleted or edited is not brought back.

## Command and permission

The command is `/treetool` (aliases `/ptree`, `/ptool`, `/proceduraltool`), for players only. One permission, **`iruutreetool.use`** (default `op`), is needed for the command, the tree tool and the placing tool.

| Command | What it does |
|---|---|
| `/treetool` or `give` | Gives the tree tool (an oak sapling) |
| `/treetool off` | Takes the tool away |
| `/treetool menu` | Opens the settings menu (choose a place with the tool first) |
| `/treetool preview` | Turns the preview on / off |
| `/treetool generate` | Puts the tree (the same as the menu's button) |
| `/treetool undo` / `redo` | The same as `//undo` / `//redo` (when WorldEdit / FAWE is there) |
| `/treetool set <key> <value>` | Sets a number. Keys: `algorithm`, `seed`, and the numbers of the pages (`/treetool info` lists them with their ranges) |
| `/treetool info` | The current settings and their numbers |
| `/treetool block <part> <block\|hand\|reset>` | Chooses the block of a part (`hand`: the block in your hand). `block leaf a,b,c` is the leaf gradient (bottom to top), `block hang a,b` the hanging strands of an ornate tree. Block states work: `block branch oak_log[axis=x]` |
| `/treetool save <name>` | Saves the current settings (lowercase letters, digits, `-` and `_`, up to 32) |
| `/treetool load <name>` / `list` / `delete <name>` | Load / list / delete (delete only moves the file to `trees/trash/`) |
| `/treetool presets` | The list of presets (click loads one, Shift+click deletes it) |
| `/treetool scatter` | Chooses the presets of the placing tool |
| `/treetool build <preset> [x y z] [seed]` | Puts a saved preset there (`~` is your own position; without coordinates, on the block you look at, else where you stand) |

## Usage

1. Get the tool with `/treetool`.
2. **Right-click a block** to open the settings of a tree standing on it (the tree's foot is the block above the one you clicked). A block far away works too — look at it and right-click in the air (`treetool.reach`, 96 blocks by default). A left click opens the menu without changing the place (before there is a place, it chooses one like a right click). Shift + right-click in the air opens the menu of the last place.
3. The pages of the menu:
   - **Basic** (基本): the place, the algorithm, the seed, loading a preset, resetting, and the **block of each part** (left-click the part's icon, then click a block in your own inventory; right-click goes back to the default).
   - **Trunk and roots / Branches / Leaves** (幹・根 / 枝 / 葉): the numbers of the algorithm (left click up, right click down, Shift for bigger steps). On the leaf page the leaves can be a **gradient of up to 6 blocks**, bottom to top.
   - **Shading** (陰影, ornate trees only): how colours are matched, ambient occlusion, curvature, light.
   - The bottom row: close / **preview** / **generate** / `//undo` / `//redo`.
4. With the **preview** on, a trial placing in the chosen blocks is shown to you alone (nobody else sees it; the world does not change). The seed is fixed, and the preview follows a change 0.75 s later (`preview-delay-ticks`).
5. When you like it, press generate. Only air and plants are replaced (terrain is safe; `replace-solid: true` changes that). A placed tree is taken back with `//undo`.

### Algorithms

| Name | What it is |
|---|---|
| `canopy` | A fractal of branches that fork by one rule; leaf clusters at the tips, roots at the foot; canopies can be stacked. Willow vines, Beta 1.7.3 style leaves (leaves far from a log vanish) |
| `tiered` | A tapering trunk with a tier of branches at a time (shorter and more upward higher up); leaf clusters on top, roots at the foot. Twist, braided trunk, snow, willow vines |
| `palm` | An S-curved trunk (it can fork) with layers of fronds on top; stilt roots |
| `ornate` (default) | Bones (a fractal of forks) given thickness by the pipe model; thick wood in logs, stairs and slabs, thin wood in walls, fences, slabs, trapdoors and fence gates joined without gaps. Bark decoration, shading, hanging strands. Styles: oak, bonsai, spruce, birch, willow, cherry, acacia. Roots can follow the ground (its height is read before placing) |
| `thin` | Grown a block at a time like a young tree. Branches thin out with the distance from the foot: logs, then walls and slabs, then fences; stairs at the joints, leaves painted in layers, willow vines |
| `silhouette` | A shape (round, weeping, conifer, swoop, dome) is drawn as a skeleton and measured block by block into logs, stairs, slabs, walls and fences. Leaves are shaded clouds, with tufts hanging below |

Switching algorithm keeps the numbers of the others.

### Presets and the placing tool

- `/treetool save <name>` writes the current settings (algorithm, numbers, seed, chosen blocks) to `plugins/IruuTreeTool/trees/<name>.yml`. The menu's "load" button (or `/treetool presets`) lists them. There is deliberately no save button in the menu, so that a stray click cannot overwrite a preset.
- The **placing tool** (a birch sapling): choose presets in "choose the placing tool's presets" (or `/treetool scatter`) — left click adds 1 to the weight, right click removes 1 — and take the tool. Right-click a block with it and one of the presets, picked by weight, is put on it with a **random seed**. Left click chooses again. Each placing is one `//undo`.
- The saved `.yml` files can be edited by hand or passed on to other servers; numbers are brought into their range when read. A file whose `kind:` is not `tree` (a rock or a house of iruuRPG itself) is not read, not listed, and left as it is.

### Sample trees (28)

- Canopy: `canopy_oak` `canopy_willow` `canopy_tiers` `canopy_beta_oak` `canopy_cherry`
- Tiered: `tiered_spruce` `tiered_snowy_pine` `tiered_braided` `tiered_weeping`
- Palm: `palm` `palm_twin` `palm_mangrove`
- Thin: `thin_birch` `thin_birch_clump` `thin_birch_split` `thin_willow`
- Silhouette: `silhouette_round` `silhouette_weeping` `silhouette_conifer` `silhouette_swoop` `silhouette_dome`
- Ornate: `ornate_fantasy` `ornate_bonsai` `ornate_spruce` `ornate_birch` `ornate_willow` `ornate_cherry` `ornate_acacia`

## Configuration (`plugins/IruuTreeTool/config.yml`)

Under `treetool:` (restart or reload after editing):

| Key | Default | Meaning |
|---|---|---|
| `max-blocks` | 60000 | Most blocks one tree may have (more and it is not put) |
| `blocks-per-tick` | 4000 | Blocks looked at and put per tick |
| `preview-delay-ticks` | 15 | Ticks to wait after the last change before the preview updates |
| `preview-blocks-per-tick` | 3000 | Preview blocks sent per tick |
| `preview-max-blocks` | 40000 | Most blocks a previewed tree may have (more and it is not shown) |
| `reach` | 96 | How far the tool reaches (8–256 blocks) |
| `replace-solid` | false | true: solid blocks (terrain) are replaced too (bedrock and barriers never) |
| `materials.<part>` | (default block of each part) | The default block of each part; what a player chose in the menu or by command comes first |

## Building

```
./gradlew build
```

Needs a Java 17 JDK (the wrapper fetches Gradle). Produces `build/libs/IruuTreeTool-1.0.0.jar` and runs the tests.

## Language

The menus and messages are in Japanese (not translated yet).

## Origin

- `canopy`, `tiered` and `palm` are ports of three of the author's own (iruuRPG) Lua tree scripts, as algorithms of this tool (the names, ranges and defaults of their numbers are the scripts').
- The `silhouette` algorithm (`silhouette/` and `SilhouetteTree`) is a port of the author's own tree generator, mc_tree_gen.
- `thin` was made after a tree-building video and a birch example.

## License

[MIT License](LICENSE) — Copyright (c) 2026 iruuRPG
