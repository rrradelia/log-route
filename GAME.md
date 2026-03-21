# 🚛 Mini Transport Tycoon — Game Guide

## What Is It?
A 2D top-down transport simulation game. Build roads, buy vehicles, create routes between cities and industries, and transport goods/passengers to earn money. Don't go bankrupt!

## Prerequisites
- **Java 21** — [Download here](https://adoptium.net/temurin/releases/?version=21)
- **Maven 3.8+** — [Download here](https://maven.apache.org/download.cgi)

Verify:
```bash
java -version    # must show 21.x
mvn -version     # must show 3.8+
```

## How to Run
```bash
cd Final/GameFinal
mvn javafx:run
```

> ⚠️ Build will fail with a clear error if Java version is below 21.

## Quick Start
1. **Build roads** — Click road mode, click tiles to lay roads connecting cities/industries
2. **Place stops** — Put stops near cities or industries (on road tiles)
3. **Create a route** — Select 2+ stops to form a circular route (A → B → C → A)
4. **Buy a vehicle** — Pick a truck or bus, assign it to a route
5. **Watch it earn** — Vehicles auto-deliver goods and passengers for income

## 💰 Economy

| Item | Details |
|------|---------|
| Starting Capital | **$50,000** |
| Bankruptcy | Capital drops below **$0** → Game Over |

### Construction Costs
| Structure | Cost |
|-----------|------|
| Road (grass) | $100 per tile |
| Road (forest) | $300 per tile |
| Wooden Bridge | $500 (max 3 tiles) |
| Stone Bridge | $1,500 (max 5 tiles) |
| Steel Bridge | $3,000 (max 10 tiles) |

### Vehicle Shop
| Vehicle | Cost | Speed | Capacity | Maintenance/cycle |
|---------|------|-------|----------|-------------------|
| Small Truck | $5,000 | 1.8 | 20 units | $60 |
| Large Truck | $12,000 | 1.0 | 60 units | $150 |
| Small Bus | $400 | 2.0 | 15 pax | $50 |
| Big Bus | $1,000 | 1.2 | 40 pax | $120 |

### Goods & Delivery Prices
| Good | Base Price/unit | Produced By |
|------|----------------|-------------|
| Wood | $8 | Forests |
| Iron | $15 | Mines |
| Paper | $20 | Factories |
| Passengers | $10 | Cities |

> Delivery income = `units × base price × city demand multiplier`

## 🗺️ Map Elements
- **Cities** (3×3+) — Generate passengers, accept goods
- **Industries** — Mine (iron), Farm, Factory (paper)
- **Forests** — Grow trees over time, source of wood
- **Rivers & Lakes** — Need bridges to cross
- **Traffic Lights** — Auto-placed at junctions, configurable green duration

## 🎮 Controls
- **Click & drag** — Pan the map
- **Scroll** — Zoom in/out
- **Sidebar buttons** — Toggle build modes (road, bridge, stop, vehicle)
- **Speed controls** — Pause | 1× | 2× | 4×

## 🏗️ Game Rules
1. Roads must connect to existing roads (no floating segments)
2. Bridges must be straight (horizontal or vertical) across water
3. Stops must be placed on road tiles adjacent to a city or industry
4. Routes need at least 2 stops and form a loop
5. Each vehicle is assigned to exactly one route
6. Max 1 vehicle per direction per road tile (overtaking allowed if faster)
7. Traffic lights control junction flow — vehicles wait on red
8. Maintenance costs are deducted automatically over time
9. Forests regrow — cutting them for roads costs 3× more

## 👥 Team
**Group 4, Team 1** — Softech Course Project
- Adelia Rustamova
- Kutmanbek Musaev
- Zuhriddin Ziyadullaev
