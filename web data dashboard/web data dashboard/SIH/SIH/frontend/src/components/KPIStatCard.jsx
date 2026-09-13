import React from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import { MiniSparkline } from './Charts';

export default function KPIStatCard({
  title,
  label,
  value,
  icon: Icon,
  trend,
  change,
  isPositive = true,
  comparisonText = 'last month',
  sparklineData = null,
  colorScheme = 'primary',
  scheme
}) {
  const getThemeStyles = () => {
    const s = colorScheme || scheme || 'primary';
    switch (s) {
      case 'success':
        return { bg: 'var(--success-light)', color: 'var(--success)' };
      case 'warning':
        return { bg: 'var(--warning-light)', color: 'var(--warning)' };
      case 'danger':
        return { bg: 'var(--danger-light)', color: 'var(--danger)' };
      case 'purple':
        return { bg: 'var(--purple-light)', color: 'var(--purple)' };
      case 'info':
        return { bg: 'var(--info-light)', color: 'var(--info)' };
      default:
        return { bg: 'var(--primary-light)', color: 'var(--primary)' };
    }
  };

  const theme = getThemeStyles();
  const defaultSpark = isPositive ? [18, 22, 28, 25, 34, 40, 48] : [48, 42, 38, 30, 26, 20, 15];

  const cardTitle = title || label || '';
  const cardTrend = trend !== undefined ? trend : (change !== undefined ? change : '+12.5%');

  // Sanitize any "vs last month", "vs last 30 days", etc. to strictly "last month"
  const rawText = comparisonText !== undefined ? comparisonText : 'last month';
  const displayComparison = String(rawText)
    .replace(/vs\s+last\s+month/gi, 'last month')
    .replace(/vs\s+last\s+30\s+days/gi, 'last month')
    .replace(/vs\s+last\s+period/gi, 'last month')
    .replace(/vs\s+last\s+week/gi, 'last month');

  return (
    <div className="kpi-card">
      <div className="kpi-top">
        <span className="kpi-title">{cardTitle}</span>
        {Icon && (
          <div className="kpi-icon-box" style={{ backgroundColor: theme.bg, color: theme.color }}>
            <Icon size={20} />
          </div>
        )}
      </div>

      <div className="kpi-middle">
        <span className="kpi-value">{value}</span>
        {cardTrend && (
          <span className={`kpi-trend ${isPositive ? 'trend-up' : isPositive === false ? 'trend-down' : 'trend-neutral'}`}>
            {isPositive ? <TrendingUp size={12} /> : isPositive === false ? <TrendingDown size={12} /> : <Minus size={12} />}
            <span>{cardTrend}</span>
          </span>
        )}
      </div>

      <div className="kpi-bottom">
        <span>{displayComparison}</span>
        <div className="sparkline-wrapper">
          <MiniSparkline data={sparklineData || defaultSpark} isPositive={isPositive} />
        </div>
      </div>
    </div>
  );
}
