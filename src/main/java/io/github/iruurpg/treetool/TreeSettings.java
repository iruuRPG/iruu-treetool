package io.github.iruurpg.treetool;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The settings of one tree: the numbers (kept in an array by {@link Key}, so the menu, the command and the yaml all read them the same way; on/off settings
 * are numbers 0 and 1), the algorithm, the seed and the blocks chosen for the parts. {@link #set} keeps every value in its range. A tree is as big as its
 * own numbers make it.
 */
final class TreeSettings {

    /** The ways a tree is grown: canopy ({@link CanopyTree}), tiered ({@link TieredTree}), palm ({@link PalmTree}), thin ({@link ThinTree}), silhouette ({@link SilhouetteTree}), ornate ({@link OrnateTree}). */
    public static final List<String> ALGORITHMS = List.of("canopy", "tiered", "palm", "thin", "silhouette", "ornate");

    /** The algorithm a fresh tree is grown by. */
    static final String DEFAULT_ALGORITHM = "ornate";

    /** A number setting: its name in the yaml and the command, the label of the menu, its range and the steps of a click and of a shift click. */
    public enum Key {
        // ---- the canopy tree (CanopyTree): a fractal of branches, canopies one above the other ----
        CANOPY_TRUNK_HEIGHT("canopy-trunk-height", "幹の高さ", 0, 40, 0, 1, 5, "最初の枝分かれまでの、まっすぐな幹の長さ (0 で根元からすぐ分かれる)"),
        CANOPY_LENGTH("canopy-length", "最初の長さ", 1, 40, 10, 0.5, 2, "樹冠の最初の枝 (主幹) の長さ。子の枝は、長さの倍率ずつ短くなる"),
        CANOPY_LENGTH_MULT("canopy-length-mult", "長さの倍率", 0.1, 1.0, 0.6, 0.05, 0.1, "子の枝の長さ = 親 × この値 (樹冠の間の幹・上の樹冠の大きさにもかかる)"),
        CANOPY_LEVELS("canopy-levels", "樹冠の数", 1, 4, 1, 1, 1, "樹冠を上へいくつ重ねるか (2 以上で、樹冠の間に幹が伸びる)"),
        CANOPY_LEVEL_DIST("canopy-level-dist", "樹冠の間隔", 0, 50, 0, 1, 5, "樹冠と樹冠の間の幹の長さに足す量 (基本 20)"),
        CANOPY_UPPER_SCALE("canopy-upper-scale", "上の樹冠の大きさ", 0.1, 3.0, 1.0, 0.05, 0.25, "2つ目より上の樹冠の、長さと太さの倍率"),
        CANOPY_RADIUS("canopy-radius", "最初の太さ", 0.1, 8.0, 2.0, 0.1, 0.5, "根元の幹の半径 (ブロック)"),
        CANOPY_RADIUS_MULT("canopy-radius-mult", "太さの倍率", 0.1, 1.0, 0.65, 0.05, 0.1, "子の枝の太さ = 親 × この値 (枝の中でも、根元から先へこの割合まで細る)"),
        CANOPY_DEPTH("canopy-depth", "枝分かれの深さ", 1, 6, 4, 1, 1, "枝が分かれる回数。一番深い枝の先に葉の塊ができる"),
        CANOPY_BRANCHES("canopy-branches", "分岐数", 1, 5, 4, 1, 1, "1つの分かれ目から出る枝の数"),
        CANOPY_SPREAD("canopy-spread", "広がりの角度", 0, 90, 45, 5, 15, "子の枝が、親の向きから開く角度 (度)"),
        CANOPY_LEAF_SIZE("canopy-leaf-size", "葉の大きさ", 1.0, 8.0, 1.5, 0.25, 1, "枝先の葉の塊の半径"),
        CANOPY_LEAF_START("canopy-leaf-start", "葉の付く深さ", 0, 3, 1, 1, 1, "この深さ以下 (先のほう) の枝には、途中にも3ブロックごとに葉が付く"),
        CANOPY_STAGGER("canopy-stagger", "枝のずれ", 0.0, 0.5, 0.0, 0.05, 0.1, "子の枝が出る位置を、枝の先から根元側へばらつかせる量 (長さに対する割合)"),
        CANOPY_TWIST("canopy-twist", "幹のねじれ", 0.0, 10.0, 0.0, 0.25, 1, "幹と最初の樹冠の主幹を、ランダムに曲げる強さ"),
        CANOPY_UPPER_TWIST("canopy-upper-twist", "上の幹のねじれ", 0.0, 1.0, 0.0, 0.05, 0.2, "樹冠と樹冠の間の幹を、ランダムに曲げる強さ"),
        CANOPY_CENTERING("canopy-centering", "中心へ戻す力", 0.0, 1.0, 0.0, 0.05, 0.2, "樹冠の間の幹が、根元の真上へ戻ろうとする強さ"),
        CANOPY_ROOT_COUNT("canopy-root-count", "根の数", 2, 8, 4, 1, 1, "根元から張り出す根の数"),
        CANOPY_ROOT_LENGTH("canopy-root-length", "根の長さ", 1.0, 15.0, 5.0, 0.5, 2, "根が外へ伸びる長さ"),
        CANOPY_ROOT_HEIGHT("canopy-root-height", "根の付け根の高さ", 0.5, 6.0, 2.0, 0.25, 1, "根が幹から出る高さ"),
        CANOPY_ROOT_SCALE("canopy-root-scale", "根の大きさ", 0.5, 5.0, 1.0, 0.1, 0.5, "根の長さ・高さ・太さの倍率"),
        CANOPY_GRAVITY("canopy-gravity", "重力", -1.0, 1.0, 0.0, 0.05, 0.2, "枝が伸びるほど下へ垂れる強さ (負で上へ反る)"),
        CANOPY_WIND("canopy-wind", "風の強さ", 0.0, 1.5, 0.0, 0.05, 0.25, "枝と幹が風下へ流れる強さ"),
        CANOPY_WIND_DIR("canopy-wind-dir", "風の向き", 0, 360, 0, 15, 45, "風が吹いていく向き (度。0 = 東、90 = 南)"),
        CANOPY_ELLIPSOID("canopy-ellipsoid", "葉を平たく", false, "葉の塊を、上下につぶれた楕円体にする"),
        CANOPY_WILLOW("canopy-willow", "柳のつる", false, "枝先の葉の塊から、葉のつるを垂らす"),
        CANOPY_WILLOW_SCALE("canopy-willow-scale", "つるの量", 0.1, 5.0, 1.0, 0.1, 0.5, "柳のつるの本数と長さの倍率"),
        CANOPY_BETA_LEAVES("canopy-beta-leaves", "ベータ版の葉", false, "Beta 1.7.3 のように、丸太から4ブロックより遠い葉を消す"),
        // ---- the tiered tree (TieredTree): a trunk with tiers of branches (conifers, snow) ----
        TIER_HEIGHT("tier-height", "木の高さ", 4, 200, 30, 1, 5, "幹の高さ (ブロック)。てっぺんに、葉の塊が5つ重なる"),
        TIER_STUMP("tier-stump", "枝のない幹", 0, 50, 8, 1, 5, "この高さまでは枝が出ない (ねじれも強め)"),
        TIER_RADIUS("tier-radius", "幹の太さ", 0.1, 8.0, 1.5, 0.1, 0.5, "根元の幹の半径 (上へ向かって細る)"),
        TIER_TWIST("tier-twist", "幹のねじれ", 0.0, 10.0, 0.0, 0.25, 1, "幹がらせんを描く強さ (0 でまっすぐ)"),
        TIER_BRAIDED("tier-braided", "編み込みの幹", false, "ねじれがあるとき、2本の幹がからみ合う"),
        TIER_BRAID_INTENSITY("tier-braid-intensity", "編み込みの強さ", 0.0, 2.0, 1.0, 0.05, 0.25, "編み込みの2本の幹が離れる量の倍率"),
        TIER_BRAID_WOBBLE("tier-braid-wobble", "枝も揺らす", true, "編み込みのとき、枝の付け根も幹のらせんに沿わせる (オフで中心から出る)"),
        TIER_ROOT_FLARE("tier-root-flare", "根の張り", 0.0, 10.0, 2.0, 0.25, 1, "根元から斜めに張り出す根の長さ (0.5 以下で根なし)"),
        TIER_SPACING("tier-spacing", "段の間隔", 1, 10, 4, 1, 1, "枝の段と段の間 (ブロック)"),
        TIER_BRANCHES("tier-branches", "1段の枝の数", 1, 21, 5, 1, 3, "1つの段から出る枝の数"),
        TIER_STAGGER("tier-stagger", "枝の高さのずれ", 0.0, 1.0, 0.7, 0.05, 0.25, "1つの段の枝の付け根を、段の間隔のこの割合の幅で上下にずらす (向きも少し崩す)。0 で全部同じ高さ"),
        TIER_BRANCH_LENGTH("tier-branch-length", "枝の長さ", 0.5, 50.0, 10.0, 0.5, 2, "一番下の段の枝の長さ (上の段ほど短い)"),
        TIER_SAG("tier-sag", "枝の垂れ", 0.0, 2.0, 0.0, 0.05, 0.25, "枝先が下へ垂れる量 (雪があるとさらに垂れる)"),
        TIER_LEAF_SIZE("tier-leaf-size", "葉の大きさ", 0.5, 8.0, 2.0, 0.25, 1, "枝に沿って付ける葉の塊の半径"),
        TIER_LEAF_DENSITY("tier-leaf-density", "葉の密度 (%)", 10, 100, 65, 5, 15, "葉の塊の中を、葉で埋める割合"),
        TIER_WILLOW("tier-willow", "柳のつる", false, "枝の外側の葉から、葉のつるを垂らす"),
        TIER_WILLOW_SCALE("tier-willow-scale", "つるの量", 0.1, 5.0, 1.0, 0.1, 0.5, "柳のつるの数と長さの倍率"),
        TIER_SNOW("tier-snow", "雪", false, "葉の上側に雪を積もらせる (雪の部位のブロック)"),
        TIER_SNOW_AMOUNT("tier-snow-amount", "雪の量", 0.0, 1.0, 0.5, 0.05, 0.2, "雪の多さ (枝の垂れも増える)"),
        TIER_WIND("tier-wind", "風の強さ", 0.0, 2.0, 0.0, 0.05, 0.25, "上へ行くほど幹が風下へ曲がる強さ"),
        TIER_WIND_DIR("tier-wind-dir", "風の向き", 0, 360, 0, 15, 45, "風が吹いていく向き (度。0 = 東、90 = 南)"),
        // ---- the palm (PalmTree): an S-curved trunk, a crown of fronds ----
        PALM_HEIGHT("palm-height", "幹の高さ", 5, 50, 16, 1, 5, "幹の高さ (ブロック)"),
        PALM_RADIUS("palm-radius", "幹の太さ", 0.5, 4.0, 1.25, 0.05, 0.25, "根元の幹の半径 (先は 0.6 まで細る)"),
        PALM_ROOT_REACH("palm-root-reach", "支柱の根", 0.0, 3.0, 0.0, 0.1, 0.5, "幹の下から、支柱のような根を張る広さ (0 で根なし)"),
        PALM_S_CURVE("palm-s-curve", "S字の曲がり", 0.0, 4.0, 0.75, 0.05, 0.25, "幹がS字に曲がる強さ"),
        PALM_SPLIT_CHANCE("palm-split-chance", "二股の確率", 0.0, 1.0, 0.0, 0.05, 0.25, "幹が途中で2本に分かれる確率 (1 で必ず)"),
        PALM_SPLIT_DIST("palm-split-dist", "二股の開き", 0.2, 3.0, 1.5, 0.1, 0.5, "分かれた2本の幹が離れる強さ"),
        PALM_LAYERS("palm-layers", "葉の層の数", 1, 5, 3, 1, 1, "てっぺんの葉の、層の数 (下の層ほど水平で、よく垂れる)"),
        PALM_FRONDS("palm-fronds", "1層の葉の数", 3, 16, 8, 1, 2, "1つの層の葉の数"),
        PALM_FROND_LENGTH("palm-frond-length", "葉の長さ", 3.0, 25.0, 7.5, 0.5, 2, "1枚の葉の長さ"),
        PALM_FROND_THICK("palm-frond-thick", "葉の厚み", 0.5, 4.0, 1.0, 0.1, 0.5, "葉の付け根の太さ (先へ細る)"),
        PALM_DROOP("palm-droop", "葉の垂れ", 0.0, 4.0, 1.5, 0.1, 0.5, "葉が先へ向かって垂れる強さ"),
        PALM_PITCH("palm-pitch", "葉の上向きの角度", 0, 90, 45, 5, 15, "一番上の層の葉が出る角度 (度。下の層ほど 18 度ずつ下がる)"),
        // ---- the thin tree (ThinTree): a trunk one block thick, branches of walls, fences and slabs, clumps over the tips with room round the wood ----
        THIN_HEIGHT("thin-height", "枝の出はじめ", 1, 30, 6, 1, 2, "根元から、横の枝が出はじめる (幹が分かれる) までの高さ。その下は枝のない幹"),
        THIN_STEMS("thin-stems", "幹の本数", 1, 3, 1, 1, 1, "根元から分かれる幹の数 (外へ開いてから上へ向き直る。2 本目からは少し低い)"),
        THIN_LEAN("thin-lean", "幹の傾き", 0.0, 1.0, 0.25, 0.05, 0.25, "根元で幹が傾く強さ (上へ伸びながら、まっすぐに戻る)"),
        THIN_CURVE("thin-curve", "幹の曲がり", 0.0, 1.0, 0.4, 0.05, 0.25, "幹がゆっくり左右に曲がる強さ (しばらく同じ側へ曲がってから戻る)"),
        THIN_BRANCHES("thin-branches", "幹の分かれ", 1, 4, 1, 1, 1, "枝の出はじめの高さで、幹が分かれる本数 (1 で分かれずに上まで 1 本)。分かれた幹は少し開いてから上へ向き直り、並んで伸びる"),
        THIN_SIDE("thin-side", "横の枝", 0, 16, 11, 1, 2, "1 本の幹から出る横の枝の数。高さをずらし、ぐるりと回りながら出る"),
        THIN_LENGTH("thin-length", "樹冠の高さ", 2.0, 30.0, 16.0, 0.5, 2, "枝の出はじめから、幹の先までの長さ (分かれたほかの幹は少し短い)"),
        THIN_DEPTH("thin-depth", "枝分かれの深さ", 0, 3, 1, 1, 1, "横の枝が、さらに小枝を出す回数 (1 回に 1〜2 本)"),
        THIN_RISE("thin-rise", "枝の立ち", 0.0, 1.0, 0.4, 0.05, 0.25, "横の枝が幹から開く角度 (0 で 70 度、1 で 15 度)"),
        THIN_KINK("thin-kink", "枝のくねり", 0.0, 1.0, 0.35, 0.05, 0.25, "枝が途中で折れ曲がる強さ"),
        THIN_LOG_REACH("thin-log-reach", "丸太の続く長さ", 0, 24, 13, 1, 2, "根元から木に沿ってこのブロック数までは丸太 (幹と、分かれた幹)。その先の幹は壁とハーフ、その 2.2 倍から先と横の枝はフェンスとハーフ"),
        THIN_FOOT("thin-foot", "根元の張り", 0, 2, 1, 1, 1, "根元のまわりに置く階段の段数 (0 でなし。木の重さを支える根元に)"),
        THIN_STAIRS("thin-stairs", "階段の継ぎ目", true, "丸太が1つ上がりながら横へずれる所を、階段でなめらかにつなぐ (オフで、上がってからずれる段)"),
        THIN_LEAF_SIZE("thin-leaf-size", "葉の塊の大きさ", 1.0, 8.0, 1.75, 0.25, 1, "葉の塊 (球のブラシ) の半径。2〜3 がよい"),
        THIN_LEAF_DENSITY("thin-leaf-density", "葉の密度 (%)", 10, 100, 40, 5, 15, "1 回の重ね塗りで、塊の中を葉で埋める割合 (30〜40% がよい)"),
        THIN_LEAF_PASSES("thin-leaf-passes", "葉の重ね塗り", 1, 5, 3, 1, 1, "葉を重ねる回数。暗い葉から明るい葉へ (葉のグラデーションの下から上)、少しずつ上に"),
        THIN_LEAF_ALONG("thin-leaf-along", "枝の途中の葉", 0.0, 8.0, 2.5, 0.5, 1, "外側の枝に沿って、このブロックごとにも葉の塊を置く (0 で枝先だけ)"),
        THIN_LEAF_FLAT("thin-leaf-flat", "葉の平たさ", 0.0, 1.0, 0.35, 0.05, 0.25, "葉の塊を上下につぶす強さ (1 で薄い傘)"),
        THIN_CLEARANCE("thin-clearance", "枝のまわりのすき間", 0.0, 2.0, 1.0, 0.25, 0.5, "枝や幹からこの距離には葉を置かない (枝先の葉の付け根は除く)。枝が葉の間から見える"),
        THIN_VINES("thin-vines", "柳のつる", false, "葉の塊の下側から、葉のつるを垂らす"),
        THIN_VINE_SCALE("thin-vine-scale", "つるの量", 0.1, 3.0, 1.0, 0.1, 0.5, "柳のつるの数と長さの倍率"),
        THIN_SIDE_LENGTH("thin-side-length", "横の枝の長さ", 0.5, 12.0, 4.5, 0.5, 2, "いちばん長い横の枝の長さ (樹冠の下から 3 分の 1 ほど。上と下ほど短く、樹冠は卵の形に)"),
        THIN_UPTURN("thin-upturn", "上への向き直り", 0.0, 1.0, 0.5, 0.05, 0.25, "幹と枝が、伸びながら上へ向き直る強さ (0 で開いたまままっすぐ。横の枝は弱く、先だけ少し上がる)"),
        // ---- the silhouette trees (SilhouetteTree: ported from the generator mc_tree_gen) ----
        SIL_SHAPE("sil-shape", "樹形", 0, 4, 0, 1, 1, "0 丸い広葉樹 / 1 しだれ / 2 針葉樹 / 3 流れる幹 / 4 傘"),
        SIL_HEIGHT("sil-height", "木の高さ", 0, 40, 0, 1, 3, "木の高さ (ブロック)。0 で樹形の標準 (丸 11・しだれ 13・針葉樹 18・流れ 10・傘 13)"),
        SIL_LEAF("sil-leaf", "葉の量", -1.0, 1.0, 0.0, 0.1, 0.5, "葉の塊の太り具合 (マイナスで痩せ、プラスで太る)"),
        SIL_ROUGH("sil-rough", "葉のでこぼこ", 0.0, 2.0, 1.0, 0.1, 0.5, "葉の表面を間引く・突き出す強さの倍率 (0 でなめらか)"),
        SIL_DRIP("sil-drip", "垂れる房", 0.0, 3.0, 1.0, 0.1, 0.5, "葉の下から垂れる房の量と長さの倍率 (しだれ・傘はもともと多い)"),
        // ---- the ornate tree (OrnateTree): bones by the pipe model, octants for the thick wood, thin wood without gaps, bark decoration, shading ----
        ORN_STYLE("orn-style", "様式", 0, 6, 0, 1, 1, "0 オーク (装飾・ファンタジー) / 1 盆栽・松 / 2 トウヒ / 3 白樺 / 4 柳 / 5 桜 / 6 アカシア。画面で変えると、形の数字はその様式の初期値になる (ブロックはそのまま)"),
        ORN_HEIGHT("orn-height", "幹の高さ", 2.0, 64.0, 9.0, 0.5, 3, "根元から、幹が枝に分かれるまでの長さ (ブロック)"),
        ORN_TRUNK("orn-trunk", "幹の太さ", 0.5, 10.0, 2.0, 0.25, 1, "根元の幹の半径 (ブロック)。枝の太さは、パイプモデルでここから決まる (半径 0.5 より細い所は、塀・フェンス・ハーフなど)"),
        ORN_CURL("orn-curl", "うねり", 0.0, 1.0, 0.5, 0.05, 0.25, "幹と枝が、ゆっくり渦を巻くように曲がる強さ"),
        ORN_ROOTS("orn-roots", "根の数", 0, 10, 5, 1, 2, "根元から外へ広がる根の数 (それぞれ途中で 2 本に分かれる)"),
        ORN_ROOT_REACH("orn-root-reach", "根の長さ", 1.0, 20.0, 6.0, 0.5, 2, "根の長さ (ブロック)"),
        ORN_FLARE("orn-flare", "根元のなめらかさ", 0.0, 3.0, 1.5, 0.25, 0.5, "幹と根の付け根を、なめらかにつなぐ幅 (ブロック)。枝分かれはなめらかにしない (鋭いまま)"),
        ORN_DECOR("orn-decor", "樹皮の装飾", 0.0, 1.0, 0.5, 0.05, 0.25, "太い幹と根の表面に付ける量: トラップドアの樹皮の筋・くぼみの階段・根の下のフェンス (0 でなし)"),
        ORN_MATCH("orn-match", "色を幹に合わせる", true, "オン: 階段・ハーフ・フェンスなども、幹のブロックに近い色を選ぶ (塀は色の近い石材) / オフ: 部位ごとに選んだブロックのまま (明暗だけ変える)"),
        ORN_SHADE_STEP("orn-shade-step", "明暗の幅", 0.0, 20.0, 7.0, 1.0, 3, "陰影の 1 段ごとに、ブロックの明るさ (L*) をどれだけ変えるか。暗い 2 段・明るい 2 段 (0 で陰影なし)"),
        ORN_AO("orn-ao", "陰影: 環境光", 0.0, 1.0, 0.6, 0.05, 0.25, "まわりを木に囲まれた所 (股・内側・下側) を暗くする強さ"),
        ORN_CURVATURE("orn-curvature", "陰影: 曲率", 0.0, 1.0, 0.3, 0.05, 0.25, "出っぱり (尾根) を明るく、くぼみを暗くする強さ"),
        ORN_LIGHT("orn-light", "陰影: 光源", 0.0, 1.0, 0.6, 0.05, 0.25, "斜め上の点光源に向いた面を明るくし、影を落とす強さ"),
        ORN_LIGHT_DIR("orn-light-dir", "光の向き", 0, 345, 135, 15, 45, "光源のある方角 (度。0 で東、90 で南、180 で西、270 で北)"),
        ORN_BRANCH_START("orn-branch-start", "最初の枝の高さ", 0.0, 64.0, 0.0, 0.5, 3, "根元から、最初の枝が出る高さ (ブロック。0 で様式どおり)。オーク: 下の枝がここから出る / 盆栽・トウヒ・白樺: 枝の段がここから始まる (幹は少なくともその 2 ブロック上まで) / 柳・桜・アカシア: 幹がここで分かれる (幹の高さの代わり)"),
        ORN_BRANCHES("orn-branches", "枝分かれの数", 2, 5, 3, 1, 1, "1 回の枝分かれで分かれる枝の数 (横に広がる型は、その 2 倍が段の数。自然木は枝の多さ)"),
        ORN_DEPTH("orn-depth", "枝分かれの深さ", 1, 6, 4, 1, 1, "枝が分かれる回数"),
        ORN_LENGTH("orn-length", "枝の長さ", 2.0, 24.0, 8.0, 0.5, 2, "最初の枝の長さ (ブロック。先へ行くほど短い)。自然木では樹冠の半径"),
        ORN_SPREAD("orn-spread", "枝の開き", 10, 90, 40, 5, 15, "分かれた枝が、元の向きから開く角度 (度)"),
        ORN_DROOP("orn-droop", "枝の垂れ", -1.0, 1.0, 0.0, 0.1, 0.5, "枝が重みで垂れる強さ (マイナスで上へ立つ)"),
        ORN_TIP_CURL("orn-tip-curl", "枝先の巻き", 0.0, 1.0, 0.5, 0.05, 0.25, "枝先が上へ巻き上がる強さ (枯れ木は巻かず鋭いまま)"),
        ORN_PIPE("orn-pipe", "太さの指数", 1.5, 3.0, 2.3, 0.1, 0.5, "パイプモデルの指数 n (親の半径^n = 子の半径^n の和)。小さいほど、枝が先へ向かって早く細くなる"),
        ORN_CROWN("orn-crown", "樹冠の種類", 0, 7, 0, 1, 1, "葉の付き方: 0 様式どおり (枝先の塊) / 1 球状 / 2 塊状 (雲のように分かれた大きな塊) / 3 棚状 (枝先ごとの平らな棚) / 4 層状 (幹のまわりの水平な段) / 5 円錐状 / 6 筆状 (下が丸く先が尖る細い炎の形) / 7 下垂状 (外側の枝から垂れる葉の幕)。円錐状・筆状は、枝も形の内側へ寄せる"),
        ORN_LEAF("orn-leaf", "葉の塊の大きさ", 0.0, 6.0, 2.5, 0.25, 1, "枝先の葉の塊の半径 (0 で葉なし。枯れ木は葉なし)"),
        ORN_LEAF_FLAT("orn-leaf-flat", "葉の平たさ", 0.0, 1.0, 0.3, 0.05, 0.25, "葉の塊を上下につぶす強さ (横に広がる型は 0.7 以上)"),
        ORN_LEAF_DENSITY("orn-leaf-density", "葉の密度 (%)", 10, 100, 85, 5, 15, "葉の塊の中を葉で埋める割合"),
        // ---- the roots on the ground, the runs of stairs, the hanging strands ----
        ORN_ROOT_SNAP("orn-root-snap", "根を地面に沿わせる", true, "オン: 根を、まわりの地面の高さに沿わせる (坂や段差でも浮かない・埋まらない) / オフ: 平らな地面と見て伸ばす"),
        ORN_STAIR_MIX("orn-stair-mix", "階段の並びを崩す", 0.0, 1.0, 0.8, 0.05, 0.25, "同じ向き・形の階段が積み重なったり 45 度に並んだりする所を、丸太・ハーフ・角の階段などに置き換える割合。縦にまっすぐ重なった同じ向きの階段は、割合によらず、いちばん上以外をフルブロックにする (0 でどちらもしない)"),
        ORN_HANG("orn-hang", "垂れる房", 0.0, 1.0, 0.0, 0.05, 0.25, "葉の塊の下 (柳は垂れた枝先) から垂れる房の多さ。ブロックは「葉」のページの下の段 (草・花なども選べる)"),
        ORN_HANG_LENGTH("orn-hang-length", "房の長さ", 1, 16, 5, 1, 3, "垂れる房のいちばん長いもの (ブロック。短いものも混ざる)");

        private final String id;
        private final String label;
        private final double min;
        private final double max;
        private final double initial;
        private final double step;
        private final double bigStep;
        private final String description;
        private final boolean toggle;

        Key(String id, String label, double min, double max, double initial, double step, double bigStep, String description) {
            this(id, label, min, max, initial, step, bigStep, description, false);
        }

        /** A setting that is on or off (1 or 0): a click turns it over. */
        Key(String id, String label, boolean on, String description) {
            this(id, label, 0, 1, on ? 1 : 0, 1, 1, description, true);
        }

        private Key(String id, String label, double min, double max, double initial, double step, double bigStep, String description, boolean toggle) {
            this.id = id;
            this.label = label;
            this.min = min;
            this.max = max;
            this.initial = initial;
            this.step = step;
            this.bigStep = bigStep;
            this.description = description;
            this.toggle = toggle;
        }

        /** The value a fresh setting starts from. */
        public double initial() {
            return initial;
        }

        /** An on/off setting (shown as オン / オフ). */
        public boolean toggle() {
            return toggle;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        public double step() {
            return step;
        }

        public double bigStep() {
            return bigStep;
        }

        public String description() {
            return description;
        }

        public boolean whole() {
            return step >= 1.0;
        }

        public static Key byId(String id) {
            for (Key key : values()) if (key.id.equalsIgnoreCase(id)) return key;
            return null;
        }
    }

    private final double[] values = new double[Key.values().length];

    {
        for (Key key : Key.values()) values[key.ordinal()] = key.initial;
    }

    private String algorithm = DEFAULT_ALGORITHM;
    /** 0 = a new random tree each time. */
    private long seed;
    private String name = "tree";
    /** The blocks chosen for the parts (the name of the material); a part not here takes the block of the config. */
    private final Map<TreePart, String> blocks = new EnumMap<>(TreePart.class);
    /** The leaf gradient, from the bottom of a clump to its top (the names of the materials); empty: the leaves are the one block of the config. */
    private final List<String> leafGradient = new ArrayList<>();
    /** The blocks of the hanging strands of an ornate tree, mixed at random (a name twice is twice as likely); empty: the lowest leaf. */
    private final List<String> hangBlocks = new ArrayList<>();

    public TreeSettings copy() {
        TreeSettings copy = new TreeSettings();
        System.arraycopy(values, 0, copy.values, 0, values.length);
        copy.algorithm = algorithm;
        copy.seed = seed;
        copy.name = name;
        copy.blocks.putAll(blocks);
        copy.leafGradient.addAll(leafGradient);
        copy.hangBlocks.addAll(hangBlocks);
        return copy;
    }

    /** The block chosen for {@code part} (a material name), or null when the config decides. */
    String block(TreePart part) {
        return blocks.get(part);
    }

    /** Chooses the block of {@code part}; null (or a name that is not a block) goes back to the config's. */
    void setBlock(TreePart part, String material) {
        String name = blockName(material);
        if (name == null) blocks.remove(part);
        else blocks.put(part, name);
    }

    List<String> leafGradient() {
        return List.copyOf(leafGradient);
    }

    /** How many shades the leaves have (1 when there is no gradient). */
    int leafShades() {
        return Math.max(1, leafGradient.size());
    }

    /** Sets shade {@code index} of the gradient (an index one past the end adds a shade); a name that is not a block is ignored. */
    void setLeafShade(int index, String material) {
        String name = blockName(material);
        if (name == null || index < 0 || index > leafGradient.size() || index >= TreePart.SHADES) return;
        if (index == leafGradient.size()) leafGradient.add(name);
        else leafGradient.set(index, name);
    }

    void removeLeafShade(int index) {
        if (index >= 0 && index < leafGradient.size()) leafGradient.remove(index);
    }

    List<String> hangBlocks() {
        return List.copyOf(hangBlocks);
    }

    /** Sets block {@code index} of the hanging strands (one past the end adds one); a name that is not a block is ignored. At most {@link TreePart#SHADES}. */
    void setHangBlock(int index, String material) {
        String name = blockName(material);
        if (name == null || index < 0 || index > hangBlocks.size() || index >= TreePart.SHADES) return;
        if (index == hangBlocks.size()) hangBlocks.add(name);
        else hangBlocks.set(index, name);
    }

    void removeHangBlock(int index) {
        if (index >= 0 && index < hangBlocks.size()) hangBlocks.remove(index);
    }

    void setHangBlocks(List<String> materials) {
        hangBlocks.clear();
        for (String material : materials) {
            String name = blockName(material);
            if (name != null && hangBlocks.size() < TreePart.SHADES) hangBlocks.add(name);
        }
    }

    /** Replaces the whole gradient (the names that are not blocks are left out; at most {@link TreePart#SHADES}). */
    void setLeafGradient(List<String> materials) {
        leafGradient.clear();
        for (String material : materials) {
            String name = blockName(material);
            if (name != null && leafGradient.size() < TreePart.SHADES) leafGradient.add(name);
        }
    }


    /**
     * The name of a block that can be placed (not air), made regular: a material ({@code OAK_LOG}), or a block with states
     * ({@code minecraft:oak_log[axis=x]}, the states as the game writes them); null when it is none. The states are checked when the block is made.
     */
    static String blockName(String material) {
        if (material == null) return null;
        String text = material.trim();
        String states = "";
        int bracket = text.indexOf('[');
        if (bracket >= 0) {
            if (!text.endsWith("]") || bracket == 0) return null;
            states = text.substring(bracket).toLowerCase(Locale.ROOT).replace(" ", "");
            text = text.substring(0, bracket);
        }
        org.bukkit.Material type = org.bukkit.Material.matchMaterial(text);
        if (type == null || !type.isBlock() || type.isAir()) return null;
        return states.isEmpty() || states.equals("[]") ? type.name() : type.getKey() + states;
    }

    /** The material of a block name (states left out), or null. */
    static org.bukkit.Material materialOf(String name) {
        if (name == null) return null;
        int bracket = name.indexOf('[');
        return org.bukkit.Material.matchMaterial(bracket >= 0 ? name.substring(0, bracket) : name);
    }

    public double get(Key key) {
        return values[key.ordinal()];
    }

    public int getInt(Key key) {
        return (int) Math.round(values[key.ordinal()]);
    }

    public void set(Key key, double value) {
        double clamped = Math.max(key.min, Math.min(key.max, value));
        values[key.ordinal()] = key.whole() ? Math.round(clamped) : Math.round(clamped * 1000.0) / 1000.0;
    }

    public void adjust(Key key, double delta) {
        set(key, get(key) + delta);
    }

    public String algorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = ALGORITHMS.contains(algorithm) ? algorithm : DEFAULT_ALGORITHM;
    }

    public void cycleAlgorithm(boolean forward) {
        setAlgorithm(ALGORITHMS.get(Math.floorMod(ALGORITHMS.indexOf(algorithm) + (forward ? 1 : -1), ALGORITHMS.size())));
    }

    /** Whether an on/off setting is on. */
    public boolean on(Key key) {
        return get(key) >= 0.5;
    }

    /** The tree of these settings is grown by {@code name}. */
    boolean grows(String name) {
        return name.equals(algorithm);
    }

    public long seed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = Math.max(0L, seed);
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null || name.isBlank() ? "tree" : name;
    }

    /** The name made safe for a file name (letters, digits, - and _), or null when nothing is left of it. */
    public static String safeName(String name) {
        if (name == null) return null;
        String clean = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return clean.isEmpty() || clean.length() > 32 ? null : clean;
    }

    /** The settings as a map for the yaml. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("algorithm", algorithm);
        for (Key key : Key.values()) map.put(key.id, key.whole() ? (Object) getInt(key) : (Object) get(key));
        map.put("seed", seed);
        Map<String, Object> chosen = new LinkedHashMap<>();
        for (Map.Entry<TreePart, String> entry : blocks.entrySet()) chosen.put(entry.getKey().key(), entry.getValue());
        map.put("blocks", chosen);
        map.put("leaf-gradient", new ArrayList<>(leafGradient));
        if (!hangBlocks.isEmpty()) map.put("hang-blocks", new ArrayList<>(hangBlocks));
        return map;
    }

    /** Reads the settings from a map (what is missing keeps the default; what is out of range is brought into it). */
    public static TreeSettings fromMap(Map<String, Object> map, String name) {
        TreeSettings settings = new TreeSettings();
        settings.setName(name);
        if (map.get("algorithm") instanceof String text) settings.setAlgorithm(text);
        for (Key key : Key.values()) {
            if (map.get(key.id) instanceof Number number) settings.set(key, number.doubleValue());
        }
        if (map.get("seed") instanceof Number number) settings.setSeed(number.longValue());
        Object chosen = map.get("blocks");
        if (chosen instanceof org.bukkit.configuration.ConfigurationSection section) chosen = section.getValues(false);
        if (chosen instanceof Map<?, ?> parts) {
            for (Map.Entry<?, ?> entry : parts.entrySet()) {
                for (TreePart part : TreePart.values()) {
                    if (part.key().equals(String.valueOf(entry.getKey()))) settings.setBlock(part, String.valueOf(entry.getValue()));
                }
            }
        }
        if (map.get("leaf-gradient") instanceof List<?> list) {
            List<String> names = new ArrayList<>();
            for (Object entry : list) names.add(String.valueOf(entry));
            settings.setLeafGradient(names);
        }
        if (map.get("hang-blocks") instanceof List<?> list) {
            List<String> names = new ArrayList<>();
            for (Object entry : list) names.add(String.valueOf(entry));
            settings.setHangBlocks(names);
        }
        return settings;
    }
}
