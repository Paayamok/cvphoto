# 浮生册正式角色视觉与桌宠动作规范

状态：视觉来源已冻结；3D 多视图、模型和动作成品待用户逐闸门验收。

## 角色身份

- 生活境：第一张母版中的蓝白浅紫古风少女。
- 行事境：第一张母版中的黑红金古风行事角色。
- 两者分别保持固定脸型、发型、发饰、服装、主色和材质层级；不得借历史图重新设计。
- 第二张只用于判断缩小后的桌宠尺度与环境关系，不得据此换脸或换装。

## 双尺度规则

每个角色只有一套正式 3D 主资产。大人物与桌宠使用同一 GLB、骨骼、材质和贴图。

- 大人物守境态：承担世界观、品牌识别和接令。
- 小桌宠执行态：缩小后移动到任务区，亲手执行照片管理动作。
- 缩放是状态变化，不是切换另一张图或另一只宠物。

## 动作清单

| 类别 | 动作 | 验收要点 |
|---|---|---|
| 常驻 | `big_idle`、`small_idle`、`blink_breathe` | 自然循环，不抢照片主体 |
| 接令 | `listen`、`acknowledge`、`think` | 有视线、点头或手势回应 |
| 化灵 | `charge`、`shrink`、`land` | 先蓄势再连续缩小，禁止跳变 |
| 移动 | `walk`、`run`、`arrive` | 脚步与位移同步，禁止滑行 |
| 生活境任务 | `open_album`、`pick_photo`、`sort_photos`、`point_result` | 明确表现翻册、取照、归整 |
| 行事境任务 | `open_scroll`、`write_plan`、`stamp_task`、`arrange_tasks` | 明确表现展卷、书写、盖印、排任务 |
| 结果 | `success`、`not_found`、`error` | 成功克制，失败有明确但不惊扰的反馈 |
| 返回 | `return_run`、`restore`、`big_idle` | 原路返回并连续恢复到守境态 |

## 状态链

`BigIdle → Listening → Acknowledge → Charge → Shrinking → Landing → Moving → TaskAction → Result → ReturnMoving → Restoring → BigIdle`

中断、撤销或任务失败也必须走安全返回链，不得瞬间消失。

## 资产交付

正式角色确认后必须同时保存：

- 3D 源文件；
- 本地 GLB；
- PBR 贴图；
- 骨骼与动作清单；
- 文件 SHA256、许可证和来源；
- Android 性能记录与模拟器录像。

任何技术样机、远程模型或候选图均不得当作正式资产。
