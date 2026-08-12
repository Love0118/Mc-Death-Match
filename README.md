# Sniper PvP workspace

소스 프로젝트와 실제 Paper 서버 런타임을 분리한 작업공간입니다.

- `project/` — Maven 소스, 테스트, 리소스팩 원본, 빌드 도구와 배포 문서
- `server/` — Paper 26.2 JAR, Via 플러그인, 서버 설정, 생성되는 월드와 로그
- `Build-Server.bat` — 프로젝트 전체 검증 후 결과물을 `server/`에 배포
- `Start-Server.bat` — `server/Start-Server.bat`을 실행

빠른 실행:

```text
Build-Server.bat
Start-Server.bat
```

`Start-Server.bat`은 사용자가 요청해 동의한 Minecraft EULA를 `server/eula.txt`에
`eula=true`로 기록하고 Java 25로 Paper 26.2를 실행합니다. ViaVersion/ViaBackwards를 통해
Minecraft 1.21.8부터 26.2까지 접속할 수 있습니다. 기본 포트는 `25565`, 최대 인원은 50명이며
서버 전송(transfer)을 허용합니다.

리소스팩은 클라이언트 프로토콜에 맞춰 1.21.8, 1.21.9~1.21.10, 1.21.11, 26.1.x,
26.2용으로 각각 빌드됩니다. DropboxAutoResourcePack이 접속자의 실제 클라이언트 버전을 확인해
해당 ZIP만 전송합니다.

경기는 자동 시작하지 않습니다. 서버 접속 후 OP가 `/sni start`로 시작하고 `/sni stop`으로
중지합니다. 승리 조건은 40킬이며 이동 속도는 0.25, 점프 강도는 0.72, 몸 크기는 1.5배로 고정합니다.
발광은 기존과 같이 적용됩니다. 체력 100과 재생되지 않는 보호막 50을 사용하며 피해는
다리 70·몸통 100·머리 150입니다. 비전투 5초 뒤부터 일반 체력만 초당 5를 회복합니다.
우클릭으로 줌을 전환하고 Q로 수동 재장전합니다.
조준 사격은 정확하며 비조준 사격에는 최대 5% 오차가 적용됩니다.

구현 세부사항과 운영 명령은 `project/README.md`를 참고하세요.
