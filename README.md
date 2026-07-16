# Restaurant Manager

A multi-tenant platform for QR-based restaurant ordering: a guest scans a table's QR code, enters their name, browses the menu, and orders from their phone. Kitchen/waiter staff see live orders and progress their status. Restaurant admins manage tables, menu, and staff. No payment integration yet (guests pay at the counter).

The repo has three parts:

```
restaurant-manager/
  src/, pom.xml, ...       Backend API (Spring Boot)
  frontend/guest-web/       Guest ordering PWA-style web app (scan QR → order)
  frontend/dashboard-web/   Staff/admin dashboard (live orders, menu, tables, staff)
```

## Backend stack

- Java 25, Spring Boot 4.1.0 (Spring Framework 7 / Jakarta EE 11)
- Spring Web, Spring Data JPA, Spring Security 7 (stateless JWT), Spring WebSocket (STOMP)
- PostgreSQL + Flyway migrations
- JWT via jjwt, QR code generation via ZXing
- springdoc-openapi (Swagger UI)
- Maven, Docker / Docker Compose

Because Spring Boot 4 shipped after this assistant's training cutoff, the code follows Spring Security 7's documented migration path (lambda DSL, `authorizeHttpRequests`, explicit CSRF disable, `spring-boot-starter-flyway`) as closely as current documentation allows, but this project has **not been compiled** in this environment (no Maven/JDK 25 or outbound access to Maven Central here). Run `mvn clean verify` locally first — expect at most minor import/version fixups given how new Boot 4 is.

## Architecture

**Multi-tenant**: every table, menu item, order, and staff account belongs to exactly one `Restaurant`. All queries are scoped by `restaurantId`.

**Two authentication audiences share one JWT filter chain:**

| Audience | How they authenticate | Token lifetime | Roles |
|---|---|---|---|
| Guest | `POST /api/v1/public/guest-sessions` with a scanned QR token + their name | 4h (configurable) | `GUEST` |
| Staff/Admin | `POST /api/v1/auth/login` with email + password | 8h (configurable) | `ADMIN`, `STAFF` |

Both token types are signed JWTs carrying a `type` claim (`STAFF`/`GUEST`), decoded by a single `JwtAuthenticationFilter` into a common `AuthPrincipal`. `SecurityConfig` restricts `/api/v1/admin/**` to `ADMIN`, `/api/v1/staff/**` to `ADMIN`/`STAFF`, and `/api/v1/guest/**` to `GUEST`.

**Guest ordering flow:**
1. Admin creates a table → gets a unique `qrToken`; `GET /api/v1/admin/tables/{id}/qrcode` returns a PNG QR code that deep-links to `{ORDER_BASE_URL}?qr={qrToken}`.
2. Guest's phone hits `GET /api/v1/public/qr/{qrToken}` to resolve the restaurant/table, then `POST /api/v1/public/guest-sessions` with their name to get a guest JWT.
3. Guest browses `GET /api/v1/public/restaurants/{id}/menu` (public, no auth) and places `POST /api/v1/guest/orders` using the guest JWT.
4. Kitchen/staff dashboard connects to `/ws?token=<staff JWT>` (STOMP over SockJS) and subscribes to `/topic/restaurants/{restaurantId}/orders` for live new-order and status-change pushes, in addition to polling `GET /api/v1/staff/orders`.
5. Staff calls `PATCH /api/v1/staff/orders/{id}/status` to move an order through `PLACED → ACCEPTED → PREPARING → READY → SERVED` (or `CANCELLED` before it's `READY`); invalid transitions are rejected with 409.

## Project layout

```
src/main/java/com/restaurantmanager/
  entity/       JPA entities + enums (Restaurant, AppUser, RestaurantTable, MenuCategory,
                MenuItem, GuestSession, Order, OrderItem, Role, OrderStatus)
  repository/   Spring Data JPA repositories (with @EntityGraph on hot read paths)
  dto/          request/ and response/ records, validated with Bean Validation
  security/     JwtService, JwtAuthenticationFilter, AuthPrincipal, entry point/denied handler
  service/      business logic + transaction boundaries
  controller/   REST controllers, grouped by audience (auth/public/guest/staff/admin-*)
  config/       SecurityConfig, WebSocketConfig (+ JWT handshake auth), OpenApiConfig, JPA auditing
  exception/    typed exceptions + a single @RestControllerAdvice
  util/         QrCodeGenerator (ZXing)
src/main/resources/
  application.yml          dev / prod / test profiles
  db/migration/V1__*.sql   Flyway schema
src/test/java/...          unit tests (order status transitions, order service) +
                            one full-flow integration test (register → table → menu →
                            QR scan → guest session → order → staff status update)
```

## Running the backend

**Option A — Docker Compose (Postgres + app):**
```bash
cp .env.example .env   # set a real JWT_SECRET
docker compose up --build
```
API available at `http://localhost:8080`.

**Option B — Maven, against a local Postgres:**
```bash
createdb restaurant_manager   # or via psql
mvn spring-boot:run           # uses the "dev" profile by default (see application.yml)
```

**Swagger UI:** `http://localhost:8080/swagger-ui.html`
**Health check:** `http://localhost:8080/actuator/health`

## Frontends

Two separate Vite + React 19 + TypeScript apps, both talking to the backend over plain REST + one STOMP/WebSocket connection (no shared build tooling between them — each has its own `package.json`).

| App | Audience | Auth | Path |
|---|---|---|---|
| `guest-web` | Guests ordering from their phone | Guest JWT (from QR scan + name, no password) | `frontend/guest-web` |
| `dashboard-web` | Kitchen/waiter staff + restaurant admins | Staff/admin JWT (email + password login) | `frontend/dashboard-web` |

Both use `axios` for REST calls; `dashboard-web` additionally uses `@stomp/stompjs` + `sockjs-client` for the live order feed on the Orders page.

**Setup (either app):**
```bash
cd frontend/guest-web        # or frontend/dashboard-web
cp .env.example .env         # VITE_API_BASE_URL, defaults to http://localhost:8080
npm install
npm run dev
```
`guest-web` runs on `http://localhost:5173`, `dashboard-web` on `http://localhost:5174` (set in each `vite.config.ts`) — different ports so you can run both at once against the same backend.

Because these were written after this assistant's training cutoff for tooling (Vite 8 and React Router v8 shipped post-cutoff with breaking changes — the removal of `react-router-dom` in v8 being the big one), both apps are deliberately pinned to versions from before the cutoff that this assistant has direct, verified knowledge of: **React 19, Vite 6, react-router-dom 6**. This trades "absolute latest" for "code that actually matches documented APIs I can reason about." Bumping to Vite 8 / React Router 8 later should be low-risk (mostly build-tooling/import-path changes) once you can run `npm install` and `npm run build` yourself to catch anything that shifted — something this sandbox couldn't do either (its `npm` registry access is blocked, same as Maven Central for the backend), so treat these two apps the same way as the backend: **not yet built/run** here, reviewed by hand instead (brace/paren balance, import-vs-usage, and DTO-shape cross-checks against the actual backend response records).

**Guest flow:** `/` reads a `?qr=<token>` URL param (what the QR code deep-links to), resolves the table, asks for a name, then routes to `/menu` → `/cart` → `/orders`. The guest JWT lives in `sessionStorage` (cleared when the tab closes, matching a single dine-in visit).

**Dashboard flow:** `/login` or `/register` (onboard a brand-new restaurant) → `/orders` (live queue, all roles) → `/menu`, `/tables`, `/staff`, `/settings` (admin-only, hidden from the nav and route-guarded for `STAFF` accounts). The staff JWT lives in `localStorage` (8h lifetime, matches the backend's `JWT_STAFF_TTL_MINUTES`).

### Running everything together

```bash
# Terminal 1
docker compose up --build          # backend on :8080

# Terminal 2
cd frontend/guest-web && npm install && npm run dev     # :5173

# Terminal 3
cd frontend/dashboard-web && npm install && npm run dev  # :5174
```

Then: open `dashboard-web` → **Set one up** to register a restaurant and admin login → **Tables** → add a table → **QR code** to see it → **Menu** → add a category + item. Copy the table's QR token (or just the deep link, `http://localhost:5173/?qr=<token>`) into a browser tab to place an order as a guest, then watch it appear live on the dashboard's **Orders** page.

## Environment variables

| Variable | Purpose | Default (dev) |
|---|---|---|
| `JWT_SECRET` | HMAC signing key, **must** be ≥32 bytes | dev-only placeholder — override for anything beyond local dev |
| `JWT_STAFF_TTL_MINUTES` | Staff/admin token lifetime | 480 |
| `JWT_GUEST_TTL_MINUTES` | Guest token lifetime | 240 |
| `ORDER_BASE_URL` | Frontend URL QR codes deep-link to | `https://order.example.com` |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Postgres connection (prod profile) | — |
| `SPRING_PROFILES_ACTIVE` | `dev`, `prod`, or `test` | `dev` |

## Demo data

Migration `V5__seed_demo_data.sql` seeds a ready-to-explore **Spice Garden**
restaurant so a freshly deployed instance isn't empty. It runs automatically via
Flyway on first start against any Postgres — including free-tier managed
databases (Neon, Supabase, Render, Railway). It seeds one restaurant, 6 tables
with QR tokens, a 15-item menu across 5 categories, and two paid orders so the
analytics dashboard has sales data on first login.

| Role | Email | Password |
|---|---|---|
| Admin | `admin@spicegarden.demo` | `password123` |
| Staff | `staff@spicegarden.demo` | `password123` |

The seed is idempotent (fixed UUIDs + `ON CONFLICT DO NOTHING`). For a real
production tenant, delete this migration before the first deploy so demo rows are
never created — or change the credentials above immediately after deploying.

To deploy: point `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` at
the managed Postgres, set `SPRING_PROFILES_ACTIVE=prod` and a real `JWT_SECRET`,
and start the app — Flyway migrates and seeds on boot.

## Quick API tour

```
POST /api/v1/auth/register-restaurant   Onboard a new restaurant + its first admin
POST /api/v1/auth/login                 Staff/admin login

GET  /api/v1/public/qr/{qrToken}                    Resolve a scanned QR code
POST /api/v1/public/guest-sessions                  Start a guest session (qrToken + name)
GET  /api/v1/public/restaurants/{id}/menu            Public menu (available items only)

GET  /api/v1/guest/session                          Current guest session info
POST /api/v1/guest/orders                           Place an order
GET  /api/v1/guest/orders[/{id}]                     Guest's own order(s)

GET/PATCH /api/v1/staff/orders[/{id}][/status]        Kitchen order queue

GET/PATCH /api/v1/admin/restaurant                    Restaurant profile
GET/POST  /api/v1/admin/staff                         Staff accounts
GET/POST/PUT/DELETE /api/v1/admin/tables              Tables (+ /{id}/qrcode PNG)
GET/POST/PUT/DELETE /api/v1/admin/menu-categories
GET/POST/PUT/DELETE /api/v1/admin/menu-items
GET       /api/v1/admin/orders                        Full order history
```

Full request/response shapes are in Swagger UI once the app is running.

## Testing

```bash
mvn test
```

- `OrderStatusTest` — status transition rules (unit)
- `OrderServiceTest` — order total calculation, unavailable-item rejection, transition validation (Mockito)
- `GuestOrderingFlowIntegrationTest` — full flow over MockMvc against an in-memory H2 database (`test` profile): register restaurant → create table/menu → scan QR → start guest session → browse menu → place order → staff accepts it → guest sees the updated status → a guest token is rejected from staff endpoints (403)

## Known trade-offs / next hardening steps

- **`spring.jpa.open-in-view` is left `true`** (Spring Boot's default) so lazy associations (e.g. `order.items`) can be read while building response DTOs in controllers. The hot read paths already use `@EntityGraph` to avoid N+1 queries; the stricter (and more work) alternative is turning OSIV off and mapping to DTOs entirely inside `@Transactional` service methods.
- **No payment integration** — orders are tracked to `SERVED`; billing happens at the counter, by design (per current scope).
- **No refresh tokens** — guest/staff JWTs simply expire; re-auth is a new login / new QR scan.
- **WebSocket auth** only gates the handshake (staff JWT required to connect); it doesn't re-check on every STOMP frame, which is standard for this scale but worth revisiting if you add per-message authorization.
- Rate limiting, audit logging, and observability (tracing/metrics beyond Actuator health) aren't included — add before real production traffic.
- **Frontends aren't built/tested here** (no npm registry access in this sandbox, same restriction as Maven Central) — run `npm install && npm run build` yourself as a first check.
- **No admin order-history UI** — `GET /api/v1/admin/orders` (full history, any status) exists and `adminApi.listAllOrders` is wired up client-side, but there's no page for it yet; the dashboard's Orders page only shows the live queue.
- Staff JWT in `localStorage` is a pragmatic choice, not the most secure one — an XSS bug would be able to read it. An httpOnly-cookie-based session is the harder-but-safer alternative if this goes to real production.
