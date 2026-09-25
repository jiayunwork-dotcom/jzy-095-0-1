# notch-fatigue — 缺口根部弹塑性应力应变核算服务

单调加载下的缺口塑性计算内核：把 **Ramberg–Osgood 本构** 与 **Neuber 双曲线** 联立，
由弹性名义应力 σn 求缺口根部的真实应力 σ 与真实应变 ε。

- 纯 HTTP/JSON 服务，无网页，不做材料牌号库/采购，不涉及晶粒屈服、稳态蠕变等模型。
- Java 17 + Spring Boot 3 + Maven，数值求根自研（对分法），**不依赖任何外部数学库**。
- 固定监听端口 **8080**。

## 计算模型

Ramberg–Osgood 单调本构（注意塑性项幂次是 **1/n**，不是 n）：

```
ε = σ/E + (σ/K)^(1/n)
```

Neuber 双曲线（弹性能与缺口根真实能的关系）：

```
σ·ε = (Kt·σn)² / E
```

联立得到关于 σ 的标量方程，在 `[0, Kt·σn]` 上严格单调、根唯一，用对分法求根：

```
f(σ) = σ²/E + σ·(σ/K)^(1/n) − (Kt·σn)²/E = 0
```

性质（均有自动化测试覆盖）：

| 情形 | 行为 |
|---|---|
| 名义应力在弹性范围 | σ = Kt·σn，ε = σ/E（精确，不进求根） |
| 进入塑性 | 真实应力增长放缓且 **σ < Kt·σn**，真实应变加快放大 |
| Kt 增大 | 同一名义应力下缺口更苛刻（σ、ε、塑性应变都增大） |
| **Kt = 1（无缺口）** | Neuber 退化回单轴 Ramberg–Osgood：σ = σn，ε = RO(σn) |

卸载只做很薄一层：基于 Masing 假设（`Δε = Δσ/E + 2·(Δσ/(2K))^(1/n)`）
与 Neuber 变程式 `Δσ·Δε = (Kt·Δσn)²/E` 联立，不追踪完整迟滞回线。

## 类的职责划分

```
constitutive/RambergOsgood.java   本构：弹性/塑性/总应变、Masing 分支、弹性判定
solver/BisectionRootFinder.java   自研对分求根；不收敛抛 NonConvergenceException
solver/NeuberSolver.java          Neuber × RO 联立（含 Kt=1 退化、弹性精确分支）
solver/ElasticReference.java      纯弹性对照基线 σ=Kt·σn, ε=σ/E
solver/NeuberUnloader.java        卸载薄壳层（Masing 分支求根）
service/NotchCalculationService.java  校验 + 单点/批量编排（各点独立求根）
service/BuiltInExample.java       内置 Kt=3 圆角缺口算例（E=206000, K=1200, n=0.2, MPa）
service/ExampleRunner.java        启动后载入算例，弹塑性结果与弹性对照一并打到日志
validation/InputValidator.java    输入校验，错误一律带原因
web/NotchController.java          HTTP 接口层
web/GlobalExceptionHandler.java   400（非法输入）/ 422（求根不收敛）
```

## HTTP 接口

### `POST /api/notch/solve` — 单点核算
```json
{
  "material": {"e": 206000, "k": 1200, "n": 0.2},
  "kt": 3,
  "nominalStress": 200
}
```
响应含 `localStress/localStrain/plasticStrain`（弹塑性解）与
`elasticStress/elasticStrain`（弹性对照）、`plastic` 标志。
上例：弹性外推 600 MPa，真实应力约 372.5 MPa —— 进入塑性后低于 Kt·σn。

### `POST /api/notch/sequence` — 批量递增序列
请求字段同单点，名义应力用 `nominalStresses: [0, 100, ...]`；
各点独立求根、互不影响，返回整段从弹性到塑性的响应。

### `POST /api/notch/unload` — 卸载薄壳层
```json
{"material":{"e":206000,"k":1200,"n":0.2},"kt":3,
 "peakNominalStress":200,"targetNominalStress":100}
```

### `GET /api/notch/example` — 内置 Kt=3 算例

### 错误
- `400 {"error":"INVALID_INPUT","reason":"..."}`：E/K/n 非正、Kt < 1、应力为负或非有限、JSON 非法等（原因明确）。
- `422 {"error":"NON_CONVERGENCE","reason":"..."}`：求根不收敛——报错而不是返回可疑数值。

## 构建、测试、运行（本地）

```bash
mvn clean test       # 运行全部自动化测试（49 个用例）
mvn spring-boot:run  # 直接运行（启动日志会打印内置算例对照表）
mvn clean package    # 打 fat jar: target/notch-fatigue-1.0.0.jar
java -jar target/notch-fatigue-1.0.0.jar
```

## Docker

```bash
docker build -t notch-fatigue .          # 镜像构建阶段会在容器内跑过全部测试
docker run --rm -p 8080:8080 notch-fatigue
curl localhost:8080/api/notch/example
```

容器内跑测试（构建阶段默认执行；也可显式进入构建镜像）：

```bash
docker run --rm notch-fatigue echo "运行阶段只含 JRE；测试已在 docker build 阶段执行"
# 如需在容器内交互式跑测试：
docker build --target build -t notch-fatigue-build .
docker run --rm -v "$PWD":/build -w /build maven:3.9-eclipse-temurin-17 mvn test
```
