import React, { useState } from 'react';

/**
 * Mini SVG Sparkline for KPI cards
 */
export function MiniSparkline({ data = [20, 28, 24, 35, 30, 42, 48], isPositive = true }) {
  const width = 80;
  const height = 26;
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;

  const points = data.map((val, i) => {
    const x = (i / (data.length - 1)) * width;
    const y = height - ((val - min) / range) * (height - 6) - 3;
    return `${x},${y}`;
  }).join(' ');

  const strokeColor = isPositive ? '#10b981' : '#ef4444';
  const fillColor = isPositive ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)';

  const areaPoints = `0,${height} ${points} ${width},${height}`;

  return (
    <svg width={width} height={height} style={{ overflow: 'visible' }}>
      <polygon points={areaPoints} fill={fillColor} />
      <polyline
        fill="none"
        stroke={strokeColor}
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
        points={points}
      />
    </svg>
  );
}

/**
 * Responsive Area Trend Chart (Pure SVG)
 */
export function AreaTrendChart({
  title = 'Trip Volume & Booking Growth',
  subtitle = 'Monthly aggregation of verified trip dispatches',
  data = [
    { label: 'Apr', value: 14 },
    { label: 'May', value: 22 },
    { label: 'Jun', value: 38 },
    { label: 'Jul', value: 45 },
    { label: 'Aug', value: 62 },
    { label: 'Sep', value: 85 }
  ],
  height = 240
}) {
  const [hoveredIdx, setHoveredIdx] = useState(null);

  const values = data.map(d => d.value);
  const min = 0;
  const max = Math.max(...values) * 1.15 || 100;
  const chartWidth = 600;
  const chartHeight = height - 40;
  const paddingX = 40;
  const paddingY = 20;

  const stepX = (chartWidth - paddingX * 2) / (data.length - 1);

  const coords = data.map((d, i) => {
    const x = paddingX + i * stepX;
    const y = chartHeight - paddingY - ((d.value - min) / (max - min)) * (chartHeight - paddingY * 2);
    return { x, y, ...d };
  });

  const linePoints = coords.map(c => `${c.x},${c.y}`).join(' ');
  const areaPoints = `${coords[0].x},${chartHeight - paddingY} ${linePoints} ${coords[coords.length - 1].x},${chartHeight - paddingY}`;

  return (
    <div className="chart-card">
      <div className="chart-header">
        <div>
          <h3 className="table-title">{title}</h3>
          <p className="table-subtitle">{subtitle}</p>
        </div>
        {hoveredIdx !== null && (
          <div style={{
            fontSize: '0.8rem',
            fontWeight: 700,
            color: '#2563eb',
            backgroundColor: 'var(--primary-light)',
            padding: '4px 10px',
            borderRadius: '6px'
          }}>
            {data[hoveredIdx].label}: {data[hoveredIdx].value} Trips
          </div>
        )}
      </div>

      <div style={{ width: '100%', overflowX: 'auto' }}>
        <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} style={{ width: '100%', height: 'auto', minWidth: '400px' }}>
          <defs>
            <linearGradient id="areaGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#2563eb" stopOpacity="0.35" />
              <stop offset="100%" stopColor="#2563eb" stopOpacity="0.0" />
            </linearGradient>
          </defs>

          {/* Grid lines */}
          {[0.25, 0.5, 0.75, 1].map((ratio, idx) => {
            const y = chartHeight - paddingY - ratio * (chartHeight - paddingY * 2);
            return (
              <g key={idx}>
                <line
                  x1={paddingX}
                  y1={y}
                  x2={chartWidth - paddingX}
                  y2={y}
                  stroke="var(--border-color)"
                  strokeDasharray="4 4"
                  strokeWidth="1"
                />
                <text
                  x={paddingX - 10}
                  y={y + 4}
                  fontSize="10"
                  fill="var(--text-muted)"
                  textAnchor="end"
                >
                  {Math.round(ratio * max)}
                </text>
              </g>
            );
          })}

          {/* Area Fill */}
          <polygon points={areaPoints} fill="url(#areaGradient)" />

          {/* Curve Line */}
          <polyline
            fill="none"
            stroke="#2563eb"
            strokeWidth="3"
            strokeLinecap="round"
            strokeLinejoin="round"
            points={linePoints}
          />

          {/* Data Points */}
          {coords.map((c, i) => (
            <g 
              key={i} 
              onMouseEnter={() => setHoveredIdx(i)}
              onMouseLeave={() => setHoveredIdx(null)}
              style={{ cursor: 'pointer' }}
            >
              <circle
                cx={c.x}
                cy={c.y}
                r={hoveredIdx === i ? 6 : 4}
                fill="#ffffff"
                stroke="#2563eb"
                strokeWidth={hoveredIdx === i ? 3 : 2}
              />
              <text
                x={c.x}
                y={chartHeight - 4}
                fontSize="11"
                fontWeight="600"
                fill="var(--text-secondary)"
                textAnchor="middle"
              >
                {c.label}
              </text>
            </g>
          ))}
        </svg>
      </div>
    </div>
  );
}

/**
 * Responsive Donut Chart (Pure SVG)
 */
export function DonutChart({
  title = 'Booking Status Distribution',
  subtitle = 'Breakdown of central reservation requests',
  data = [
    { label: 'Accepted', value: 21, color: '#10b981' },
    { label: 'Pending', value: 8, color: '#f59e0b' },
    { label: 'Completed', value: 14, color: '#3b82f6' },
    { label: 'Rejected', value: 3, color: '#ef4444' }
  ]
}) {
  const total = data.reduce((sum, d) => sum + d.value, 0) || 1;
  const radius = 65;
  const strokeWidth = 22;
  const circumference = 2 * Math.PI * radius;

  let accumulatedPercent = 0;

  return (
    <div className="chart-card">
      <div className="chart-header">
        <div>
          <h3 className="table-title">{title}</h3>
          <p className="table-subtitle">{subtitle}</p>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '24px', flexWrap: 'wrap', padding: '10px 0' }}>
        <div style={{ position: 'relative', width: '160px', height: '160px' }}>
          <svg width="160" height="160" viewBox="0 0 160 160" style={{ transform: 'rotate(-90deg)' }}>
            {data.map((item, idx) => {
              const strokeDasharray = `${(item.value / total) * circumference} ${circumference}`;
              const strokeDashoffset = -accumulatedPercent * circumference;
              accumulatedPercent += item.value / total;

              return (
                <circle
                  key={idx}
                  cx="80"
                  cy="80"
                  r={radius}
                  fill="transparent"
                  stroke={item.color}
                  strokeWidth={strokeWidth}
                  strokeDasharray={strokeDasharray}
                  strokeDashoffset={strokeDashoffset}
                  strokeLinecap="round"
                />
              );
            })}
          </svg>
          <div style={{
            position: 'absolute',
            inset: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            textAlign: 'center'
          }}>
            <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--text-primary)', lineHeight: 1 }}>{total}</span>
            <span style={{ fontSize: '0.65rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase', marginTop: '3px' }}>Bookings</span>
          </div>
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          {data.map((item, idx) => {
            const pct = Math.round((item.value / total) * 100);
            return (
              <div key={idx} style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.8rem' }}>
                <span style={{ width: '10px', height: '10px', borderRadius: '50%', backgroundColor: item.color, flexShrink: 0 }}></span>
                <span style={{ color: 'var(--text-secondary)', minWidth: '80px' }}>{item.label}</span>
                <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>{item.value} ({pct}%)</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

/**
 * Responsive Bar Chart (Pure SVG)
 */
export function BarTrendChart({
  title = 'Top Destination Performance',
  subtitle = 'Trips and inquiries recorded by destination circuit',
  data = [
    { label: 'Manali & Solang', value: 48, secondary: '₹1.2M' },
    { label: 'Goa Coast', value: 72, secondary: '₹1.8M' },
    { label: 'Jaipur Heritage', value: 34, secondary: '₹890k' },
    { label: 'Munnar Backwaters', value: 26, secondary: '₹640k' }
  ]
}) {
  const max = Math.max(...data.map(d => d.value)) || 1;

  return (
    <div className="chart-card">
      <div className="chart-header">
        <div>
          <h3 className="table-title">{title}</h3>
          <p className="table-subtitle">{subtitle}</p>
        </div>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', paddingTop: '6px' }}>
        {data.map((item, idx) => {
          const pct = Math.round((item.value / max) * 100);
          return (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.825rem' }}>
                <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{item.label}</span>
                <span style={{ color: 'var(--text-secondary)', fontWeight: 600 }}>{item.value} Trips <span style={{ color: 'var(--text-muted)' }}>({item.secondary})</span></span>
              </div>
              <div style={{
                width: '100%',
                height: '8px',
                backgroundColor: 'var(--bg-surface-secondary)',
                borderRadius: '999px',
                overflow: 'hidden'
              }}>
                <div style={{
                  width: `${pct}%`,
                  height: '100%',
                  background: 'linear-gradient(90deg, #2563eb 0%, #38bdf8 100%)',
                  borderRadius: '999px',
                  transition: 'width 0.6s ease'
                }} />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
