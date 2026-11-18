# V3.3 Service 层逐文件课程答辩问答

本章初版根据源码版本 `21fe14ca8f22416f8a8bf7bc3516f9c45f181c3d` 编写，本轮已同步 ServiceFactory 与密码匹配调用调整，现覆盖 3 个接口、3 个实现类和 1 个工厂，共 52 题。问题同时面向软件构造中的抽象、契约、重构、异常、函数式表达，以及系统分析与设计中的用例、权限、状态和一致性。文中的场景表是按源码推演的答辩例子，不是本次实际运行记录；“可改进”均指未来方案，不代表当前已经实现。

21 个公开业务方法的覆盖如下；3 个 `getInstance()` 属于实例获取方法，另在单例相关题中解释。

| 接口 | 公开方法 | 主要题号 |
| --- | --- | --- |
| UserService | `login`、`register`、`requireResourceAccess` | S14、S16、S17 |
| ResourceService | `addResource`、`queryAllResources`、`queryAvailableResources` | S21、S22 |
| ResourceService | `queryResourcesByName`、`queryResourceById`、`requireResourceForModification` | S23—S25 |
| ResourceService | `modifyResource`、`deleteResource` | S26、S27 |
| ReserveRecordService | `requireStudentAccess`、`requireAdminReserveAccess`、`parseReserveTime` | S31、S32 |
| ReserveRecordService | `submitReserve`、`queryMyReserves`、`cancelReserve` | S33—S35、S36、S38—S39 |
| ReserveRecordService | `queryAllReserves`、`queryPendingReserves`、`queryReserves` | S36、S37 |
| ReserveRecordService | `auditReserve` | S40—S45 |

共通知识可结合 [阅读导航与核心知识](00-阅读导航与核心知识.md) 学习；持久化对象和文件语义见 [DAO 与实体问答](01-DAO与实体逐文件问答.md)、[工具类问答](04-工具类逐文件问答.md)。

## src/service/UserService.java

源码：[UserService.java](../../../src/service/UserService.java)。职责：定义登录、注册和进入资源功能前的角色检查。关键方法：`login`、`register`、`requireResourceAccess`。类型依赖：`User`、`Result`、`IOException`；没有 DAO、Scanner 或文件路径依赖。

### S01. 为什么要把 UserService 写成接口，它抽象的究竟是什么？

**答：**接口抽象的是调用者需要的用户业务能力：提供账号和密码来登录、注册，以及判断当前用户是否允许进入资源功能。它不规定用户放在文件还是数据库，也不规定控制台怎样提问。`ConsoleSession` 保存 `UserService` 类型的字段，因此只需要知道这些操作及其返回值，具体查找、校验和保存由实现承担。从系统分析角度，这三个操作对应参与者的业务行为；从软件构造角度，它形成稳定的调用边界。当前接口仍公开实体 `User` 和统一结果 `Result`，所以只是隐藏实现细节，并不是调用方与所有数据结构完全解耦。

### S02. 为什么只有 register 声明 throws IOException，业务异常为什么没有写在签名中？

**答：**`register` 会执行 `users.save()`，当前实现将文件保存的 `IOException` 交给调用者处理，因此接口保留这个受检异常契约。通过 `UserService` 引用调用注册时，调用者必须捕获它或继续声明抛出。`login` 查询已加载的内存数据，不执行保存，因而没有同样的声明。账号为空、密码错误、权限不足等异常继承 `BusinessException`，而后者继承 `RuntimeException`，属于非受检异常，省略声明仍可抛出。“没有 throws”不能解释成“方法不会失败”。接口签名展示的是编译期必须处理的异常要求，而完整业务契约还包括成功条件与运行时失败条件。

### S03. UserService userService = ServiceFactory.getUserService() 中哪里体现多态，哪里不是？

**答：**`ConsoleApplication` 先通过 `ServiceFactory.getUserService()` 取得接口类型的引用，工厂内部返回 `UserServiceImpl.getInstance()`。随后 `userService.login(...)` 根据对象实际类型执行 `UserServiceImpl.login`，这才是接口引用下的运行时多态。工厂获取方法和 `getInstance()` 都是静态方法，不依靠对象的运行时类型完成重写分派。两步承担不同职责：静态入口负责取得对象，接口方法负责执行业务。具体实现的选择集中在 ServiceFactory，控制台组装和会话处理代码都不需要导入 Service 实现类。

### S04. 能不能直接把原来的单例类改成 abstract class 来实现抽象，为什么当前选择接口？

**答：**不能只加一个 `abstract` 关键字而保留原样。当前单例字段直接执行 `new UserServiceImpl()`，抽象类不能被直接实例化，这一行就不能编译；私有构造器也不是供外部子类扩展的正常入口。真正改成抽象基类，需要重新安排子类、构造器和实例创建，属于另一种设计。这里要隔离的是“调用者能做什么”，没有要求不同实现共享一套受保护状态或模板算法，用接口更直接，也避免占用实现类唯一的类继承位置。接口和单例分别回答能力契约与实例数量问题，可以同时存在。当前接口有三个抽象方法，也不是可以由一个 lambda 实现的函数式接口。

## src/service/ResourceService.java

源码：[ResourceService.java](../../../src/service/ResourceService.java)。职责：定义资源增删改查及修改前置检查。关键方法共 8 个，见本章覆盖表。依赖 `CampusResource`、`User`、`List`、`Result` 和 `IOException`；角色、资源状态以及预约关联约束由实现兑现。

### S05. 这个接口为什么不只保留一个通用的 execute 方法，八个方法如何对应业务用例？

**答：**独立方法把意图和参数写在类型契约里：新增需要名称、类型、位置、描述；修改还需要资源 ID 和目标状态；删除只需 ID；查询分为管理员全部、学生可用、名称和 ID 四种路径。`requireResourceForModification` 则给控制台提供先验证目标、再询问新字段的能力。如果统一成 `execute(String action, Object... args)`，动作拼写和参数顺序错误往往延迟到运行期，业务用例也不易从接口读出。当前八个方法都围绕校园资源，具有领域内聚性；但接口本身不会自动保证权限，必须由每个实现方法执行对应检查。

### S06. 为什么返回 Result<List<CampusResource>>、Result<CampusResource> 和 Result<Void>，空列表是否代表失败？

**答：**泛型描述业务结果的数据形状：多条查询返回资源列表，新增、修改或按 ID 查询返回一个资源，删除只需成败消息，所以用 `Result<Void>` 表示没有业务数据。`Void` 不是方法返回类型 `void`：方法仍然返回一个可读取成功标志与消息的 `Result` 对象。当前列表查询即使没有匹配项，也返回成功结果及非空引用的空列表；这表示“查询已完成，结果为零条”。按 ID 查询找不到则返回 `Result.fail`。非法角色、非法参数等又走异常路径。因此不能把“数据为空”“结果失败”和“抛出异常”三者混为一谈，调用方需要按具体契约解释。

### S07. 接口里的 User 参数是否意味着管理员能调用任何资源方法，参数规范化由谁完成？

**答：**不是。实现规定管理员能新增、修改、删除和查询全部资源；`queryAvailableResources` 却只允许学生，管理员直接调用也会被拒绝。按名称和 ID 查询允许两种角色，但学生只能看到 `AVAILABLE` 资源。接口没有用不同 Java 类型区分管理员与学生，所以权限属于运行时前置条件。控制台会对名称、关键词和 ID 执行 `trim()`，Service 的部分校验只检查 `null` 和空字符串，并不统一去除空白。答辩时应区分“经控制台输入时的表现”和“直接调用接口时的表现”，不能把 UI 的规范化误写成接口已经保证的契约。

### S08. 从具体类提取这个接口时，怎样判断是重构，而不是悄悄改变了业务行为？

**答：**重构应保留调用者能观察到的行为。例如新增、修改、删除仍声明 `IOException`，权限错误仍先于后续参数错误，学生查询仍隐藏不可用资源，列表仍按 ID 排序，消息和失败形式也应一致。仅把控制台字段类型改为接口、给实现添加 `implements` 和 `@Override`，可以改变依赖组织而保留业务语义。若顺便让管理员调用学生专用查询、把空列表改成失败、把保存异常改成普通返回值，就已经改变了契约。应分别说明这些需求变化，而不是把它们归为“抽取接口时的代码优化”。比较前后结果时还要检查保存后的数据及失败后的内存状态。

## src/service/ReserveRecordService.java

源码：[ReserveRecordService.java](../../../src/service/ReserveRecordService.java)。职责：定义学生提交、查询和取消预约，以及管理员审核和查询预约。共 10 个公开方法，其中包括两项访问检查和时间解析。依赖 `User`、`ReserveRecord`、`LocalDateTime`、`List`、`Result`，不直接暴露 DAO。

### S09. 十个方法如何反映参与者与用例，parseReserveTime 为什么也放在业务接口里？

**答：**学生路径是 `requireStudentAccess`、`submitReserve`、`queryMyReserves`、`cancelReserve`；管理员路径是 `requireAdminReserveAccess`、`queryAllReserves`、`queryPendingReserves`、`auditReserve`、`queryReserves`。时间解析服务于预约输入，把统一格式和错误提示交给 Service，控制台只负责读取字符串。这样避免控制台复制日期解析规则，但也让接口包含了一项输入转换能力，未达到严格意义上“只描述领域命令”的边界。对当前小型课程系统这是现有职责分配；未来若新增图形界面直接提交时间对象，可以考虑独立转换器，而不必为了形式上的分层现在就增加一层。

### S10. 预约也写文件，为什么接口完全没有 throws IOException，这是否代表比资源接口更安全？

**答：**原因是异常被转换了，而不是文件操作变得可靠了。提交路径通过 `saveReserves` 捕获 `IOException` 并抛出 `BusinessException`；审核和取消通过 `updateReserveStatus` 捕获保存错误，尝试恢复内存及部分文件状态，再抛出业务异常。调用者不必在编译期专门处理 `IOException`，但仍可能收到保存失败。相比之下，用户注册和资源修改直接传播受检异常。两种策略在当前代码中并存，不能声称整个系统已经统一异常边界。转换后的提示更贴近业务文件，但当前异常构造只传消息，没有保留原异常作为 cause，也限制了进一步诊断的信息。

### S11. 这个接口的前置条件、后置条件和业务不变式分别是什么？

**答：**以提交为例，角色为学生、资源存在、开始早于结束、结束未早于当前、无已通过预约冲突且资源可用，构成成功前置条件；正常返回后新增一条 `PENDING` 记录并完成预约保存，是成功后置条件。取消则要求本人记录且允许转为 `CANCEL`。业务不变式描述跨操作希望一直成立的关系，例如同一资源不能存在时间重叠的 `PASS` 预约、有已通过预约时资源应维持 `OCCUPIED`。这些条件由实现共同维护，接口声明本身不会证明它们成立。公开实体 setter、文件外部修改、并发和保存中断都可能绕过或破坏这些约束，答辩应说明保证范围。

### S12. 实现类使用了 lambda，为什么不能说 ReserveRecordService 本身就是函数式接口？

**答：**函数式接口需要满足单一抽象方法的要求，而该接口声明了十个不同抽象操作，不能用一个 lambda 直接实例化。实现类中的 lambda 是传给 `Predicate`、`Supplier` 等函数式接口的行为参数，与业务接口是否为函数式接口是两件事。`ReserveConsole` 依赖业务接口，使它可以接收另一实现或测试替身；但当前 `ReserveRecordServiceImpl` 的私有构造器仍主动调用静态 DAO 工厂，替换 Service 内部存储并不方便。接口降低了 UI 对实现的耦合，却没有自动完成所有层次的依赖注入，更没有让带保存副作用的预约命令变成纯函数。

## src/service/impl/UserServiceImpl.java

源码：[UserServiceImpl.java](../../../src/service/impl/UserServiceImpl.java)。职责：实现账号校验、登录、学生注册和基础角色门禁。关键业务方法 3 个；私有辅助方法为 `isUsernameValid`、`isPasswordValid`。依赖 `UserDao` 接口，通过 `DaoFactory` 获取实际存储对象。

### S13. 这里的单例由哪三部分组成，它保证的范围是什么？

**答：**`private static final INSTANCE` 在类初始化时创建对象，私有构造器阻止普通外部代码直接 `new`，`public static getInstance()` 返回同一个引用，构成饿汉式单例。构造器把 `DaoFactory.getUserDao()` 的结果保存到 `final users` 字段；`final` 保证字段引用不被重新赋值，不表示 DAO 内部集合不可修改。Java 类初始化机制负责初始化阶段的同步，但业务方法没有同步，因此“单例对象初始化安全”不能推导出“注册查重与保存线程安全”。通常讨论的是同一类加载器中的单一实例，且依赖 DAO 的首次初始化仍可能加载文件并失败，不能说取单例永远没有副作用或失败。

### S14. login 为什么按当前顺序检查，账号不存在且密码也不正确时会发生什么？

**答：**顺序是账号非空白、密码非空、按用户名查找、用户存在、密码相等，最后返回成功及 `User`。每次抛出异常都会立即终止，因此账号为空时不会先提示密码错误；合法格式账号不存在时会抛 `UserNotFoundException`，不会进入密码比较。密码比较委托给 `user.matchesPassword(password)`，由 User 内部判断输入是否与保存值相等；该方法对 null 输入返回 false。登录没有调用注册用的六位密码校验，所以文件里已有的短密码仍可能正确登录。该函数只校验凭据并返回用户，没有在 Service 内维护“当前登录用户”；会话由 UI 持有返回对象继续完成。

### S15. 两个私有校验函数到底验证了什么，为什么不能把它们讲成完整的账号安全规则？

**答：**`isUsernameValid` 检查非 `null` 且 `trim()` 后不为空，但返回布尔值，并没有把修剪后的字符串写回。直接传入 `" alice "` 时，查找与保存仍使用带空格的原值。`isPasswordValid` 检查非 `null`、非空且长度至少 6，不检查字符种类，也不去空白，所以六个空格满足当前注册条件。用户名查找和密码比较都是精确比较，没有忽略大小写。提取函数的价值是为条件命名和复用，不会自动增加安全性。当前密码也按字符串直接比较，不能称为密码哈希验证；若要加强规范，应先明确新需求与已有账号兼容策略。

### S16. register 的完整流程是什么，保存失败后是否等于没有注册过？

**答：**它先验证用户名，再验证密码长度，然后查重；通过后构造 `User(username, password, "student", LocalDateTime.now())`，将对象加入 DAO，再调用 `save()`，最后返回成功。角色固定为学生，所以接口没有让普通注册者自选管理员权限。问题在于 `add` 已经改变内存，随后保存若抛 `IOException`，本方法没有移除用户或恢复列表。因此 UI 即使显示保存失败，同一进程再次注册该账号仍可能得到“账号已存在”。这正是命令失败语义与成功语义都必须分析的例子：当前保证正常返回前尝试并完成保存，不保证异常退出时回到调用前状态。

### S17. requireResourceAccess 是查询、命令还是权限断言，为什么不直接返回 boolean？

**答：**它不修改用户和文件，检查角色为 `admin` 或 `student`，否则抛 `PermissionDeniedException`，属于通过正常返回表示条件成立的权限断言。使用 `void` 可以让调用者直接继续正常流程，失败则进入统一异常处理，不必每个调用点都写布尔分支。`"admin".equals(user.getRole())` 使角色字段为 `null` 时比较结果为假，但 `user` 本身为 `null` 仍会触发空指针；这个写法不是全面的空安全。它只控制进入资源功能的基础资格，不能替代每个资源或预约操作的精细权限检查，也没有验证传入用户一定来自真实登录会话。

### S18. 为什么 login 声明返回 Result，失败却抛异常，错误究竟在哪里显示？

**答：**当前 `login` 的正常路径返回 `Result.success`，参数错误、用户不存在和密码错误通过各自业务异常离开。`ConsoleSession.run` 捕获 `BusinessException` 后再构造 `Result.fail(e.getMessage())`，交给输出组件显示，所以屏幕上失败结果可能来自 UI 的转换，并不是 `login` 返回了失败结果。注册的 `IOException` 由另一 catch 转成用户数据保存失败消息。分层后 Service 不直接 `System.out.println`，便于复用业务逻辑；但返回值失败与异常失败混用也要求调用者清楚约定，不能仅检查 `result.isSuccess()` 就以为覆盖了所有错误分支。

### S19. 这个类怎样进一步提高可测试性，哪些改变不能冒充保持行为的重构？

**答：**可考虑让构造过程显式接收 `UserDao`，默认入口仍由工厂装配，从而在测试中提供可控的内存 DAO 或保存必失败的替身；时间也可在需要稳定断言时作为依赖处理。这些属于未来设计，当前私有构造器和静态单例没有提供此入口。重构前应固化账号、密码校验顺序及失败后的内存表现等现有行为。给注册增加回滚、把密码改为哈希、统一去除账号空格都可能合理，但会改变调用者可见结果，应作为有独立需求和验证的行为变更。不能因为接口已经存在，就声称已经能方便地隔离真实文件测试该实现。

## src/service/impl/ResourceServiceImpl.java

源码：[ResourceServiceImpl.java](../../../src/service/impl/ResourceServiceImpl.java)。职责：实现资源权限、查询可见性、增删改及已通过预约约束。8 个业务方法之外，私有辅助方法有 `collectResourceQuery`、`findResourceById`、`generateResourceId`、`isResourceStatusValid`、`ensureAdmin`。持有 `ResourceDao` 与 `ReserveRecordDao` 两个接口依赖。

### S20. 资源服务为什么同时依赖资源 DAO 和预约 DAO，这是否破坏了分层？

**答：**资源修改和删除必须知道是否存在该资源的 `PASS` 预约，只读取资源自身字段不能完成这个判断。因此 Service 协调两个 DAO，把跨实体业务规则放在业务层，是当前分层的合理职责。DAO 负责提供数据与保存机制，不应通过文件工具自行决定谁能删除资源。该类仍用私有构造器、静态单例和 `getInstance()`，两个依赖由 `DaoFactory` 隐式取得；字段类型依赖接口，但实例选择仍固定为工厂提供的对象。单例让多处调用共享状态，也扩大了测试间状态干扰的可能，并没有提供两份数据共同提交的事务机制。

### S21. addResource 为什么先判断管理员再判断名称，ID 和初始状态怎样产生？

**答：**顺序是 `ensureAdmin`、名称非 `null` 且非空、生成 ID、创建资源、加入 DAO、保存、返回成功。先检查权限意味着学生即使传入全 `null` 参数也首先收到权限错误，控制台正利用这一点阻止学生伪选管理员操作。`generateResourceId` 用 `UUID.randomUUID().toString()`，并循环查找，直到当前 DAO 没有同 ID；这是当前集合内查重，不是跨进程的唯一约束。新资源状态固定为 `AVAILABLE`。名称检查没有 `trim`，也没有验证类型、位置和描述；保存失败后没有撤销添加，必须如实说明这些校验边界和失败副作用。

### S22. queryAllResources 和 queryAvailableResources 的差别是否只是多了一个 filter？

**答：**不只是过滤条件不同。前者先调用 `ensureAdmin`，仅管理员允许查看全部；后者显式要求学生，管理员直接调用也失败，然后只留下状态字符串等于 `AVAILABLE` 的资源。两者都将流交给 `collectResourceQuery`，统一按资源 ID 排序并返回成功的列表结果。空列表只改变提示文字，不变成失败。`AVAILABLE` 是当前保存的粗粒度状态，这里没有预约时间参数，也不计算指定时间段是否空闲，所以界面中的“可预约”实际含义是“资源当前标记可用”，不能解释为任意未来区间的可预约性已被完整计算。

### S23. queryResourcesByName 的两个 filter 分别承担什么职责，关键词有哪些边界？

**答：**它先计算用户是否为管理员，拒绝管理员和学生之外的角色，再检查关键词非 `null`、非空。第一层 `filter` 处理可见性：管理员保留全部状态，学生只保留 `AVAILABLE`；第二层使用 `resource.getName().contains(keyword)` 做名称子串匹配。角色许可与文本匹配分开，便于读出“先限定可看数据，再找名称”的逻辑。匹配区分大小写，没有分词、拼音或正则语义；Service 不主动 `trim`，所以空格关键词并非空字符串。捕获的 `admin`、`keyword` 是未被再次赋值的局部变量或参数，符合 lambda 对捕获变量的限制，谓词本身没有修改资源。

### S24. queryResourceById 为什么有时返回 Result.fail，有时抛异常，私有查找函数做了什么？

**答：**公开方法先验证角色，不允许的角色抛权限异常；随后 `findResourceById` 调用 DAO 查找，找到就包装成功结果，未找到则返回失败结果。学生找到非 `AVAILABLE` 的资源时，公开方法再次返回“未找到该 ID 对应的资源”，从业务可见性上隐藏这条资源。管理员可以取得任意状态资源。这里没有单独的“ID 为空”验证，因此不能编造对应的参数异常。私有方法把 `null` 转为明确结果对象，方便查询复用；修改与删除又把同样的查找失败转成 `ResourceNotFoundException`，说明失败形式取决于用例，而不只是底层是否找到数据。

### S25. requireResourceForModification 和 modifyResource 重复检查权限与存在性，是不是应直接删除一个？

**答：**不能只因看见重复就删除。控制台先读取 ID，调用 `requireResourceForModification`，确认管理员权限且资源存在后才询问新名称等字段，这避免用户为无效目标填写整组信息。真正的 `modifyResource` 又重新检查，确保直接调用该业务方法时也受保护，而不是依赖 UI 曾经检查过。预检查正常返回只说明当时满足条件，并不锁住资源，也不是后续操作必定成功的承诺。可以在不改提示和检查顺序的前提下考虑提取内部共同逻辑，但两个公开入口服务于不同调用时机，保留业务方法自己的校验是正确的边界意识。

### S26. modifyResource 的检查顺序和成功后的修改范围是什么，它维护了哪条预约约束？

**答：**顺序是管理员权限、资源存在、名称非空、状态合法、已通过预约约束，然后才改对象和保存。`isResourceStatusValid` 只接受 `AVAILABLE`、`OCCUPIED`、`DISABLED` 三个精确字符串。如果目标不是 `OCCUPIED` 且存在同资源 `PASS` 记录，就抛业务异常；目标是 `OCCUPIED` 时利用 `&&` 短路跳过这次预约扫描。成功时只更新名称、类型、位置、状态，ID 和描述保持原值。约束是“有 PASS 就不能改成其他状态”，并不反向要求“没有 PASS 就不能手工设为 OCCUPIED”。四个 setter 之后保存失败不会恢复旧字段，这与预约状态更新的处理不同。

### S27. deleteResource 怎样防止破坏已通过预约，删除后其他预约记录会怎样？

**答：**方法先确认管理员，再查资源并要求存在，随后对预约列表执行 `anyMatch`：只要同资源存在一条 `PASS`，就拒绝删除。没有这类记录时，从资源 DAO 移除对象并保存资源文件；不会级联删除预约记录，也不保存预约文件。因此关联 `PENDING`、`REJECT`、`CANCEL` 的资源仍可以删除，历史记录中的资源 ID 可能变成悬空引用。后续待审记录尝试通过审核时，会由预约服务报关联资源不存在；驳回并不需要资源存在。这是当前关联完整性的局限，不能宣称系统实现了数据库外键约束。删除保存失败时同样没有把资源重新放回内存。

### S28. collectResourceQuery 为什么统一收集和排序，方法引用与比较器分别是什么？

**答：**该函数把不同查询产生的 `Stream<CampusResource>` 统一执行 `sorted(Comparator.comparing(CampusResource::getId))` 和 `collect(Collectors.toList())`，再根据空或非空选择提示。公共查询保留各自权限与筛选，私有函数负责统一输出形态，避免重复排序和包装逻辑。`CampusResource::getId` 是方法引用，可理解为接收资源并返回其 ID 的函数；`Comparator.comparing` 据此建立按字符串自然顺序比较的比较器。排序结果是 ID 字典顺序，不是新增时间顺序，UUID 的排列不能当成时间排序。这里没有 `map`，因为始终保留资源对象，只筛选、排序并收集，没有把每个元素转换成另一种数据。

### S29. Stream 的惰性和 anyMatch 的短路在这里有什么实际意义，是否意味着零开销？

**答：**`filter`、`sorted` 构建中间处理链，终结操作 `collect` 才驱动元素处理；`anyMatch` 是可短路的终结操作，找到满足条件的记录后不必继续匹配，空流返回 `false`。排序为了产出有序结果仍需收集和比较数据，不能像单纯匹配那样见到第一条就完成。[Java 8 Stream 文档](https://docs.oracle.com/javase/8/docs/api/java/util/stream/Stream.html) 定义了这些执行语义。结合项目 DAO 考察，`findAll()` 已先复制一个列表，所以即使后面的 `anyMatch` 很早停止，前面也已经付出了复制成本。当前是顺序流，没有 `parallelStream()`，不能把改写为 Stream 等同于自动并行或自动提高性能。

### S30. 资源查询符合命令查询分离吗，返回新列表为什么仍可能泄露可修改状态？

**答：**查询方法本身只读取、筛选和包装结果，不调用保存；增删改是会改变内存和文件的命令，这体现了基本的职责区分。但返回新列表不等于返回不可变快照：DAO 的 `findAll()` 复制的是列表容器，`collect` 也没有深拷贝 `CampusResource`，元素仍是同一批可变实体，调用者可通过 setter 绕过 Service 修改它们。按 ID 查询也直接返回 DAO 对象。因此当前只能说查询过程不主动修改业务状态，不能说 API 从类型上保证无副作用。未来可考虑只读视图、DTO 或复制策略，同时应单独处理新增、修改、删除保存失败时均未回滚的现状。

## src/service/impl/ReserveRecordServiceImpl.java

源码：[ReserveRecordServiceImpl.java](../../../src/service/impl/ReserveRecordServiceImpl.java)。职责：实现预约提交、审核、取消、查询以及跨资源和预约的业务协调。持有 `ReserveRecordDao`、`ResourceDao`，使用单例和共享格式常量。私有函数按职责分组：提交校验与保存、资源可审核和释放判定、查询收集与存在性判断、ID 生成、状态迁移和补偿保存、角色检查；以下逐组展开。

### S31. 构造器、格式常量和四个角色检查入口分别解决什么问题？

**答：**私有构造器通过 `DaoFactory` 取得预约与资源 DAO，静态 `INSTANCE` 与 `getInstance()` 提供共享业务对象；`RESERVE_TIME_FORMAT` 统一输入时间模式。公开 `requireStudentAccess` 委托私有 `ensureStudent`，两者要求学生；公开 `requireAdminReserveAccess` 与私有 `ensureAdmin` 都要求管理员，但异常提示不完全相同。控制台在读取预约参数前调用公开检查，具体业务方法仍自行检查，以覆盖直接调用场景。`queryPendingReserves` 使用公开管理员检查，而全部查询、条件查询和审核使用私有管理员检查。重构若统一这些函数，还要注意提示差异也是可观察行为；检查仍假设 `User` 对象非空。

### S32. parseReserveTime 的成功和失败边界是什么，是否严格拒绝所有无效日期和 null？

**答：**方法对字符串执行 `trim()`，按 `yyyy-MM-dd HH:mm` 解析为 `LocalDateTime`；只捕获 `DateTimeParseException` 并转换为格式错误业务异常。`value` 为 `null` 时会在 `trim()` 处发生空指针，不在这个 catch 范围内。格式器由 `ofPattern` 创建，默认解析处理风格是 SMART，代码没有显式启用严格解析，所以不能把“指定了格式”当作“已严格拒绝所有无效日历组合”。格式器本身不可变且可共享，见 [Java 8 DateTimeFormatter 文档](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html)。解析只负责转换，并不验证预约先后顺序、是否过期或资源冲突；这些留给提交校验。

### S33. submitReserve 从调用到正常返回经历哪些步骤，为什么提交后不把资源设为 OCCUPIED？

**答：**流程是检查学生角色、`validateSubmission`、生成预约 ID、用用户名和当前时间构造 `PENDING` 记录、`addAndSaveReserve`、返回成功。`generateReserveId` 与资源 ID 一样用 UUID 并循环查重，避免当前列表内重复。备注原样传入，Service 没有限制必须填写。申请人与角色来自传入 `User`，没有另去用户 DAO 校验身份。提交只是形成待审核申请，不代表资源已获批准使用，因此没有修改或保存资源状态。只要资源仍为 `AVAILABLE` 且没有已通过冲突，多个学生可提交相互重叠的 `PENDING`；是否批准由审核阶段重新判断。

### S34. validateSubmission 为什么必须逐条读，代码是否要求开始时间必须晚于现在？

**答：**实际顺序是查资源并要求存在、要求 `start.isBefore(end)`、拒绝 `end.isBefore(LocalDateTime.now())`、检查已通过时间冲突、要求资源状态为 `AVAILABLE`。它没有要求 `start` 晚于现在，所以“开始已过去、结束仍在未来”的区间可能通过，不能把结束时间条件改述为“只能预约未来开始的时间”。也没有显式检查 `start`、`end` 为 `null`。若资源不存在且时间也非法，先报资源不存在；若既重叠又非可用，先报时间冲突。现在时间在方法内部实时取得，等于边界的判断必须相对于该次读取的精确时刻，不能只看界面显示到分钟的值。

### S35. 已通过预约的时间冲突为什么用两个严格小于，端点相接算冲突吗？

**答：**`hasPassConflict` 只看同资源且状态为 `PASS` 的记录，并要求新区间开始早于旧区间结束、旧区间开始早于新区间结束，两者同时成立才冲突。它对应半开区间 `[start, end)` 的重叠判断：旧预约 10:00—11:00，新预约 11:00—12:00 不重叠；10:30—11:30、09:00—12:00、相同区间都重叠。`PENDING`、`REJECT`、`CANCEL` 不参与占用判定，待审申请并没有提前锁定时段。下面是当前代码的核心条件，不能随意把 `<` 改为 `<=`，否则相接时段的可用性发生业务变化。

```java
return records.findAll().stream().anyMatch(record -> resourceId.equals(record.getResourceId())
        && record.getStatus() == ReserveStatus.PASS
        && start.isBefore(record.getReserveEndTime())
        && record.getReserveStartTime().isBefore(end));
```

### S36. queryMyReserves、queryAllReserves 和 queryPendingReserves 的权限、范围和顺序如何区分？

**答：**个人查询先要求学生，再用用户名精确匹配申请人，返回该学生的所有状态记录；全部查询要求管理员，不过滤记录；待审核查询要求管理员，只筛选 `PENDING`。三者都由 `collectReserveQuery` 收集为列表，空列表仍是成功并使用对应空结果消息。这个收集函数与资源服务不同，没有 `sorted`，所以保留源列表的遇见顺序，不能声称按申请时间、ID 或状态排序。当前 DAO 的列表通常体现加载及追加顺序，但接口没有约定它是时间排序。三条查询路径不写文件，返回元素仍是可变预约实体，并非深拷贝的历史快照。

### S37. queryReserves 如何把字符串查询条件转为筛选逻辑，哪些输入会先失败？

**答：**它先要求管理员，再检查查询值非 `null`、非空，然后处理 `field`：`"1"` 按资源 ID 精确匹配，`"2"` 按申请人精确匹配，`"3"` 通过 `ReserveStatus.valueOf(value)` 转枚举并比较状态；其他字段抛查询条件无效。状态拼写不匹配时捕获 `IllegalArgumentException` 并给出合法状态提示。没有大小写转换或去空格，因此直接传 `pass` 或 `" PASS "` 不等于 `PASS`；控制台会提前 trim。若字段和值同时非法且值为空，先报查询值不能为空。它采用分支选择一条过滤链，没有做模糊查询或多个条件组合查询。

### S38. cancelReserve 为什么先查存在，再检查本人，最后检查状态，Optional 在哪一步起作用？

**答：**顺序是学生权限、通过 ID 找记录、不存在即抛业务异常、确认申请人为当前用户名、验证可转 `CANCEL`、判定是否需要释放资源、执行更新保存。先验证所有权意味着别人已经取消的记录仍首先报“只能取消本人的预约”，不会先泄露为状态错误。`Optional.ofNullable(...).orElseThrow(...)` 把 DAO 的可能空结果转换为显式失败，异常供应 lambda 只在空值时调用；它没有验证 `user`、时间字段或实体内部字段是否为空。[Java 8 Optional 文档](https://docs.oracle.com/javase/8/docs/api/java/util/Optional.html) 说明它只是可能含有非空值的容器，不是自动消除整个调用链所有空指针的机制。

### S39. findResourceToRelease 为什么只在取消最后一条 PASS 时释放，other != record 比较的是什么？

**答：**取消 `PENDING` 时原本没有占用，函数直接返回 `null`，不读取或修改资源。取消 `PASS` 时必须先找到关联资源，找不到就抛异常，然后扫描同资源的其他 `PASS`。只要还有一条，返回 `null` 让更新函数只取消预约；没有其他已通过记录才返回资源，使其改为 `AVAILABLE`。`other != record` 比较对象引用，排除正在取消的那一个实例。它在当前 DAO 返回列表浅拷贝且按 ID 返回内部对象的条件下成立；若将来 DAO 改为每次复制实体，就不能未经检验照搬该假设。这段判断不按时间过滤，任何其他 `PASS` 都会阻止释放。

### S40. auditReserve 的 decision 校验为什么值得单独提问，非法选项一定先报选项错误吗？

**答：**并不一定。真实顺序是管理员权限、记录存在、根据 `"2"` 推导 `REJECT` 否则推导 `PASS`、`requireTransition`、检查选项只能是 `"1"` 或 `"2"`，然后进入驳回或通过分支。对 `PENDING` 传入非法选项 `"9"`，推导的 `PASS` 转移允许，随后报选项无效；对已经 `PASS` 的记录传同样选项，则先被状态检查拒绝，报不能重复审核。`null` 选项也按这个顺序处理，因为比较采用常量字符串的 `equals`。未来把选项检查提前可能更直观，但会改变多重错误输入下的首个异常和消息，应作为明确行为调整而非无差别重构。

### S41. 审核驳回为什么不检查资源是否仍存在，驳回原因和审核时间怎样保存？

**答：**审核选项为 `"2"` 且状态迁移允许后，只要求驳回原因非 `null` 且 `trim()` 后非空，再调用 `updateReserveStatus(record, REJECT, null, null, rejectReason)`。驳回不产生占用，因此没有查资源、检查冲突或保存资源的必要；即使资源已被删除，待审申请仍可结束为 `REJECT`。校验虽使用 `trim()`，实际保存的是原始原因，前后空格没有被主动移除。更新函数写入 `REJECT`、原因和当前审核时间，随后保存预约文件。失败会恢复旧状态、旧原因和旧时间。已驳回记录不能再次审核，也不能取消，这由统一迁移规则决定。

### S42. requireApprovableResource 为什么允许某些 OCCUPIED 资源通过审核，与提交规则存在什么差异？

**答：**它先要求资源存在，再检查同资源 `PASS` 的时间重叠，然后计算是否存在任意 `PASS`。资源为 `AVAILABLE` 可以继续；为 `OCCUPIED` 且确有其他 `PASS` 时也允许继续，因为前面的重叠判断已排除了时间冲突；`DISABLED` 及没有 `PASS` 支撑的 `OCCUPIED` 都拒绝。这使提前提交的非重叠待审申请可以陆续通过。可是新提交的 `validateSubmission` 无条件要求 `AVAILABLE`，因此资源一旦被某条预约标为 `OCCUPIED`，新的不重叠未来预约仍无法提交。这是粗粒度状态与时间段模型不一致的局限；审核还没有重新验证结束时间是否已过期，不能替代码补上这条规则。

### S43. requireTransition 实际允许哪四条状态边，四种状态的十六种组合如何判断？

**答：**允许的只有 `PENDING → PASS`、`PENDING → REJECT`、`PENDING → CANCEL` 和 `PASS → CANCEL`。`REJECT`、`CANCEL` 都没有出边，同状态重复操作也不允许。布尔表达式依靠 `&&` 高于 `||` 的优先级，把待审三种去向与已通过取消两部分相连。非法目标为 `CANCEL` 时提示当前不可取消；目标为 `PASS` 或 `REJECT` 时提示不能重复审核；其他目标走通用非法变更提示。状态合法只是必要条件，还需满足权限、本人、资源及时间冲突等约束，不能把表中“允许”理解为调用必然成功。

| 当前状态 \ 目标状态 | PENDING | PASS | REJECT | CANCEL |
| --- | --- | --- | --- | --- |
| PENDING | 拒绝 | 允许：审核通过 | 允许：审核驳回 | 允许：本人取消 |
| PASS | 拒绝 | 拒绝 | 拒绝 | 允许：本人取消 |
| REJECT | 拒绝 | 拒绝 | 拒绝 | 拒绝 |
| CANCEL | 拒绝 | 拒绝 | 拒绝 | 拒绝 |

### S44. updateReserveStatus 为什么一次保存三个预约旧字段，取消时为什么不清空审核信息？

**答：**它先记录旧预约状态、旧驳回原因、旧审核时间，以及需要更新时的旧资源状态，然后修改预约状态。只有目标为 `PASS` 或 `REJECT` 时才写入传入的原因和当前审核时间；取消只改状态，保留已有审核信息。因此通过后取消仍能看到原审核时间，待审取消通常仍没有审核时间。通过分支传空字符串作为原因，驳回传实际原因，取消传 `null` 却不会因此清掉原字段。若保存失败，必须一起恢复这些字段，否则会出现“状态回到待审但已经有新审核原因或时间”的混合状态。集中更新有助于让多个业务入口共享同一恢复逻辑。

### S45. 两个 save 和失败补偿为什么不等于事务，reserveSaved 标志能证明什么？

**答：**更新先保存预约，正常返回后置 `reserveSaved=true`，有资源变更时再保存资源。发生 `IOException` 就恢复内存；若预约保存曾正常返回，再尝试把恢复后的预约重写一次。该标志只说明第一次预约保存调用正常结束，不证明磁盘写入原子、掉电可靠或两个文件一致。底层 `FileUtil` 用覆盖式 `FileOutputStream` 写文件，异常前可能已截断或写入部分内容；资源写失败后也没有重写旧资源文件。因此这是尽力补偿，不是具备原子性的跨文件事务；若补偿再失败，会给出两个文件都需检查的消息。

| 失败位置 | 内存处理 | 预约文件处理 | 仍存在的风险 |
| --- | --- | --- | --- |
| 首次 `records.save()` 抛错 | 恢复已改字段 | 不再尝试重写，标志仍为 false | 预约文件可能已部分写入 |
| `resources.save()` 抛错，预约补偿成功 | 恢复预约及资源字段 | 重写旧预约状态 | 资源文件可能部分写入，未执行资源文件补偿 |
| `resources.save()` 抛错，预约补偿也抛错 | 内存已恢复 | 恢复尝试失败 | 两个文件均可能与内存不一致 |
| 无资源更新，仅预约保存成功 | 保留新预约状态 | 保存新状态 | 属于单文件成功，不涉及第二次保存 |

### S46. addAndSaveReserve 与 saveReserves 为什么拆成两个函数，它们的回滚和用户注册有什么区别？

**答：**`saveReserves` 专注把 DAO 的 `IOException` 转成预约文件保存失败的 `BusinessException`；`addAndSaveReserve` 则先添加记录，捕获这类业务异常后移除刚添加的对象，再重新抛出。前者处理错误表达，后者维护本次提交的内存补偿，使职责比在提交主流程中混杂更清楚。它保证这条指定异常路径下不把新记录留在内存，却没有恢复可能已经损坏的预约文件，也没有捕获所有类型的运行时异常。与此相对，用户注册和资源增删改没有同样的内存恢复逻辑。不能笼统地说“项目所有保存失败都会回滚”，更不能把移除内存对象说成完整事务回滚。

### S47. 这个类的 lambda、Optional 和 Stream 属于哪种函数式用法，为什么整个业务服务仍不是纯函数？

**答：**`filter` 的 lambda 表达筛选谓词，`anyMatch` 的 lambda 表达存在性条件，`orElseThrow` 的 lambda 延迟提供异常；`collect` 把流结果归集为列表。`hasPassRecord` 只检查同资源存在 `PASS`，`hasPassConflict` 还增加区间重叠，不能把两者混成同一条件。本类没有使用 `map`，也没有把实体映射为另一类型。局部谓词主要读取字段，但整个服务依赖可变 DAO、`LocalDateTime.now()` 和随机 UUID，并修改对象与文件，所以同样输入不保证同样输出或状态，不满足纯函数要求。函数式表达改善局部意图，并不消除业务状态、I/O 和命令副作用。

### S48. 能否完整追踪一次涉及两条预约的提交、审核与取消，验证资源为什么最后才释放？

**答：**下面是源码推演：假设管理员与学生甲、乙均合法，资源 R 初始可用，两个区间均在当前时间之后，DAO 原无 R 的 `PASS`，各次保存成功。甲、乙必须先完成待审提交，再审核，才能展示当前对非重叠待审申请的支持；如果甲已经通过后乙才新提交，乙会在资源状态检查处失败。取消第一条已通过记录时仍有另一条占用，不能提前把 R 标记可用；最后一条取消才释放。记录 A、B 保留创建时的申请信息；取消后也保留通过审核时记录的审核时间。表中 A、B 只是便于讲解的代号，实际记录 ID 为 UUID。

| 步骤 | 调用与关键判断 | A 状态 | B 状态 | R 状态与保存 |
| --- | --- | --- | --- | --- |
| 1 | 甲提交明天 10:00—11:00；合法且无 PASS | PENDING | 未创建 | AVAILABLE；只保存预约 |
| 2 | 乙提交明天 11:00—12:00；A 待审不占用 | PENDING | PENDING | AVAILABLE；只保存预约 |
| 3 | 管理员按 `"1"` 审核 A | PASS | PENDING | OCCUPIED；先预约后资源 |
| 4 | 管理员按 `"1"` 审核 B；端点相接无冲突，存在其他 PASS | PASS | PASS | OCCUPIED；先预约后资源 |
| 5 | 甲取消 A；仍找到同资源 B 为 PASS | CANCEL | PASS | OCCUPIED；只保存预约 |
| 6 | 乙取消 B；已无其他 PASS | CANCEL | CANCEL | AVAILABLE；先预约后资源 |

如果第 2 步把 B 改为 10:30—11:30，提交仍可成功，但第 4 步会因 A 已通过而冲突，B 保持 `PENDING`；管理员可再以非空理由驳回它。这一分支说明冲突检查必须出现在审核时，不能只依赖提交时曾经通过校验。

### S49. 现有实现在哪些条件下维护了不变式，哪些边界不能靠单例和 Service 自己保证？

**答：**在单线程、数据初始有效、调用都经过这些方法且保存成功的正常路径上，审核阻止重叠 `PASS`，资源修改阻止有 `PASS` 时改为其他状态，取消最后一条 `PASS` 才释放。保证范围之外仍有缺口：实体 setter 可直接修改状态，角色来自可变 `User`，文件可被外部改变；单例也没有把“查冲突—修改—保存”变成不可分割操作，并发审核可能交错。结束后的预约不会自动改状态或释放资源，审核也没有重新拒绝过期记录。`LocalDateTime` 不携带时区，不能单独标识全球唯一时刻，见 [Java 8 LocalDateTime 文档](https://docs.oracle.com/javase/8/docs/api/java/time/LocalDateTime.html)。这些是当前模型边界，不能用“用了面向对象”来掩盖。

### S50. 如果老师要求重构这个长类，怎样拆才有理由，又怎样保留可观察行为？

**答：**先按现有职责识别边界：权限断言、输入校验、时间冲突查询、状态迁移、状态更新与持久化补偿。当前已用私有函数分开很多步骤，不必为了减少行数机械增加类。若存储测试困难，可先让依赖显式注入；若迁移规则继续增加，再考虑独立规则对象。验证时要保持首个异常、消息、查询顺序、审核信息保留、保存调用顺序及失败后的状态，尤其不能漏掉“先迁移后 decision”的当前行为。将资源状态改为按时段计算、补齐日期规则或换成真正事务都可能是合理改进，但会改变业务与失败契约，应单独提出方案和验证，不应偷偷夹在结构重排中。

### S51. 如果为预约服务设计验证，哪些用例比只测一条成功路径更能证明理解了代码？

**答：**应围绕边界和分支建立可控数据，而不是只看菜单能运行。角色错误与空参数同时出现可验证检查优先级；相同、包含、部分重叠与首尾相接区间可验证冲突公式；非本人、待审、通过、驳回、取消组合可验证迁移与所有权。还应验证两个文件的三种失败位置，以及审核时间、原因、资源状态是否按预期恢复。当前隐式工厂使替换失败 DAO 不方便，这是测试设计会暴露的构造问题。以下列的是从源码推出的预期用例，并未声称本次已经执行；真实测试应使用隔离数据和可控制时间，记录实际输出再比较。

| 验证焦点 | 代表输入或安排 | 当前应观察到的结果 |
| --- | --- | --- |
| 时间先后 | `start == end`，资源存在 | 开始必须早于结束，未新增记录 |
| 允许的时间边界 | 开始早于当前、结束晚于当前，其他条件合法 | 当前规则允许提交 |
| 粗粒度状态 | 资源 OCCUPIED，但新申请与 PASS 不重叠 | 仍拒绝新提交 |
| 决策顺序 | 已 PASS 记录，`decision="9"` | 先报不能重复审核 |
| 所有权顺序 | 其他学生的 CANCEL 记录 | 先报只能取消本人预约 |
| 末条释放 | 同资源两条 PASS，依次取消 | 第一次仍占用，第二次可用 |
| 字段保留 | PASS 后取消 | 原审核时间保持不变 |
| 内存补偿 | 首次预约保存或资源保存失败 | 对应字段恢复，另检查磁盘不确定状态 |

## src/service/ServiceFactory.java

源码：[ServiceFactory.java](../../../src/service/ServiceFactory.java)。职责：集中获取三个服务单例，分别以 `UserService`、`ResourceService`、`ReserveRecordService` 接口类型返回。关键方法为 `getUserService()`、`getResourceService()`、`getReserveRecordService()`；私有构造器阻止普通实例化。

### S52. ServiceFactory 为什么返回接口，它是否重新创建单例或实现了运行时多态？

**答：**工厂内部调用三个实现类各自的 `getInstance()`，将实现选择集中在一处，使 ConsoleApplication 只通过接口接收服务。每次获取仍返回对应实现维护的同一个实例，工厂没有另建业务对象，也没有增加新的缓存或依赖注入框架。静态工厂调用本身不是运行时多态；之后经接口引用调用 `login`、`submitReserve` 等实例方法时，才执行实际实现的重写方法。这与 DaoFactory 的职责相对应，保留当前单例和分层约定。

本章覆盖检查：7 个源码文件、52 个连续题号、21 个公开业务方法，另解释 3 个单例获取方法、3 个工厂获取方法及全部私有职责组。关键限制包括权限参数缺少独立身份认证、可变对象外泄、粗粒度资源状态、时间规则不完整、存储依赖隐式固定、用户与资源写入无回滚、预约补偿不具备跨文件原子性；这些结论均须与当前实现范围一起陈述。
