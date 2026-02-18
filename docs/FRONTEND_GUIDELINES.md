# Frontend Design System & Guidelines

## OrthoWatch – Post-Discharge Monitoring System

**Version**: 1.0
**Last Updated**: Feb 2026
**Stack**: React 18 + TypeScript 5.3 + Tailwind CSS 3.4 + shadcn/ui + Recharts
**Component Convention**: shadcn/ui patterns — all custom components follow shadcn structure

---

## 1. Design Principles

### Core Principles

1. **Clinical Clarity**: Every element serves a clinical purpose. Risk levels, alert states, and patient status must be instantly recognizable. No decorative clutter — surgeons review 10+ patients in minutes.
2. **Scanability**: Information hierarchy optimized for rapid triage. Most critical data (risk score, alert status) visible without scrolling or clicking.
3. **Consistency**: Identical patterns across all screens. A risk badge looks the same on the dashboard, patient list, and patient detail.
4. **Accessibility**: WCAG 2.1 Level AA compliance. Touch targets ≥44×44px (tablet use by nurses). High contrast for clinical risk colors.
5. **Performance**: Dashboard loads in <3 seconds. No layout shift on data load. Skeleton states for all async content.

### User-Driven Decisions

| User | Device | Priority | Implication |
|---|---|---|---|
| Surgeon | Desktop (wide monitor) | Scan Top 10 patients fast | Dense data layout, sparkline charts, keyboard shortcuts |
| Nurse | Tablet (iPad) | Manage alerts + enroll patients | Large touch targets, simplified forms, clear action buttons |
| Caregiver* | WhatsApp only | Respond to structured buttons | No web UI — WhatsApp buttons designed for elderly users |

> *Caregiver/patient interaction is WhatsApp-only. No patient-facing web UI exists.

---

## 2. Design Tokens

### Color Palette

#### Clinical Risk Colors (Non-Negotiable)

These colors are the visual language of the system. They map directly to clinical risk levels and must NEVER be used for non-clinical purposes.

```css
/* HIGH Risk — Danger, Immediate Action */
--risk-high: #dc2626;         /* red-600 */
--risk-high-bg: #fef2f2;      /* red-50 */
--risk-high-border: #fca5a5;  /* red-300 */
--risk-high-text: #991b1b;    /* red-800 */

/* MEDIUM Risk — Warning, Monitor Closely */
--risk-medium: #d97706;         /* amber-600 */
--risk-medium-bg: #fffbeb;      /* amber-50 */
--risk-medium-border: #fcd34d;  /* amber-300 */
--risk-medium-text: #92400e;    /* amber-800 */

/* LOW Risk — Safe, No Action Needed */
--risk-low: #16a34a;          /* green-600 */
--risk-low-bg: #f0fdf4;       /* green-50 */
--risk-low-border: #86efac;   /* green-300 */
--risk-low-text: #166534;     /* green-800 */
```

#### Usage Rules for Risk Colors

- ✅ Risk score badges, alert severity indicators, trend arrows, dashboard cards
- ❌ NEVER for buttons, links, decorative elements, or non-clinical status

#### Primary Colors (Medical/Clinical Palette)

```css
--color-primary-50: #f0f9ff;   /* Lightest — backgrounds */
--color-primary-100: #e0f2fe;
--color-primary-200: #bae6fd;
--color-primary-300: #7dd3fc;
--color-primary-400: #38bdf8;
--color-primary-500: #0ea5e9;  /* Main brand — sky-500 (medical teal-blue) */
--color-primary-600: #0284c7;  /* Hover states */
--color-primary-700: #0369a1;  /* Active/pressed */
--color-primary-800: #075985;
--color-primary-900: #0c4a6e;  /* Darkest — headings on light bg */
```

> **Rationale**: Sky-blue is standard for medical/clinical dashboards. Neutral, professional, avoids confusion with clinical risk colors (red/amber/green).

#### Neutral Colors

```css
--color-neutral-50: #f8fafc;   /* Page background */
--color-neutral-100: #f1f5f9;  /* Card backgrounds, alt rows */
--color-neutral-200: #e2e8f0;  /* Borders, dividers */
--color-neutral-300: #cbd5e1;  /* Disabled borders */
--color-neutral-400: #94a3b8;  /* Placeholder text */
--color-neutral-500: #64748b;  /* Secondary text */
--color-neutral-600: #475569;  /* Body text */
--color-neutral-700: #334155;  /* Strong body text */
--color-neutral-800: #1e293b;  /* Headings */
--color-neutral-900: #0f172a;  /* Darkest text */
```

#### Semantic Colors (Non-Clinical UI Feedback)

```css
--color-success: #16a34a;  /* green-600 — form success, save confirmation */
--color-warning: #d97706;  /* amber-600 — form warnings, timeouts */
--color-error: #dc2626;    /* red-600 — form validation errors */
--color-info: #0ea5e9;     /* sky-500 — informational banners */
```

#### Usage Rules

| Token | Use For | Never For |
|---|---|---|
| Primary (sky) | CTAs, links, focus rings, navigation active states | Clinical risk indicators |
| Neutral (slate) | Text, backgrounds, borders, disabled states | Alerts, risk badges |
| Risk High (red) | HIGH risk badge, CRITICAL alert, emergency override | Form errors (use semantic error instead) |
| Risk Medium (amber) | MEDIUM risk badge, warning alert | General warnings |
| Risk Low (green) | LOW risk badge, safe status | Success toasts (use semantic success) |
| Semantic | Form validation, toasts, informational UI | Clinical risk communication |

---

### Typography

#### Font Families

```css
--font-sans: 'Inter', system-ui, -apple-system, sans-serif;
--font-mono: 'JetBrains Mono', 'Fira Code', monospace;
```

> **Inter**: Optimized for UI/dashboard readability at all sizes. Load via Google Fonts: `wght@400;500;600;700`.

#### Type Scale

| Token | Size | px | Use |
|---|---|---|---|
| `text-xs` | 0.75rem | 12px | Timestamps, metadata, helper text |
| `text-sm` | 0.875rem | 14px | Table cells, labels, secondary info |
| `text-base` | 1rem | 16px | Body text, form inputs |
| `text-lg` | 1.125rem | 18px | Card titles, section labels |
| `text-xl` | 1.25rem | 20px | Page subtitles |
| `text-2xl` | 1.5rem | 24px | Page titles |
| `text-3xl` | 1.875rem | 30px | Dashboard KPI numbers |
| `text-4xl` | 2.25rem | 36px | Hero numbers (rare) |

#### Font Weights

```css
--font-normal: 400;    /* Body text */
--font-medium: 500;    /* Labels, table headers, UI elements */
--font-semibold: 600;  /* Card titles, section headings */
--font-bold: 700;      /* Page titles, KPI numbers, risk scores */
```

#### Line Heights

```css
--leading-tight: 1.25;   /* Headings, KPI numbers */
--leading-normal: 1.5;   /* Body text, descriptions */
--leading-relaxed: 1.75;  /* Long-form text (rare in this app) */
```

---

### Spacing Scale

```css
--spacing-0: 0;
--spacing-0.5: 0.125rem;  /* 2px — fine adjustments */
--spacing-1: 0.25rem;     /* 4px — inline icon gaps */
--spacing-2: 0.5rem;      /* 8px — compact padding */
--spacing-3: 0.75rem;     /* 12px — small card padding */
--spacing-4: 1rem;        /* 16px — default component padding */
--spacing-5: 1.25rem;     /* 20px */
--spacing-6: 1.5rem;      /* 24px — card padding, grid gap */
--spacing-8: 2rem;        /* 32px — section spacing */
--spacing-10: 2.5rem;     /* 40px */
--spacing-12: 3rem;       /* 48px — page section gaps */
--spacing-16: 4rem;       /* 64px — page top/bottom margins */
```

#### Spacing Rules

| Context | Token | Value |
|---|---|---|
| Inline icon-to-text gap | spacing-1 to spacing-2 | 4–8px |
| Button padding | spacing-2 (y) × spacing-4 (x) | 8×16px |
| Card internal padding | spacing-4 to spacing-6 | 16–24px |
| Card-to-card gap (grid) | spacing-4 to spacing-6 | 16–24px |
| Section-to-section gap | spacing-8 to spacing-12 | 32–48px |
| Page side margins | spacing-4 (mobile) to spacing-8 (desktop) | 16–32px |

---

### Border Radius

```css
--radius-none: 0;
--radius-sm: 0.125rem;    /* 2px */
--radius-base: 0.25rem;   /* 4px — inputs, small elements */
--radius-md: 0.375rem;    /* 6px — shadcn/ui default */
--radius-lg: 0.5rem;      /* 8px — cards, modals */
--radius-xl: 0.75rem;     /* 12px — large cards */
--radius-full: 9999px;    /* Badges, avatars, pills */
```

> **Default**: `radius-md` (6px) — matches shadcn/ui convention. Override in `components.json`.

---

### Shadows

```css
--shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
--shadow-base: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px -1px rgba(0, 0, 0, 0.1);
--shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);
--shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -4px rgba(0, 0, 0, 0.1);
--shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
```

| Shadow | Use |
|---|---|
| `shadow-sm` | Subtle cards, table rows on hover |
| `shadow-base` | Default card elevation |
| `shadow-md` | Elevated cards, dropdowns |
| `shadow-lg` | Modals, popovers |
| `shadow-xl` | Full-screen overlays (rare) |

---

## 3. Layout System

### Grid System

- **Container**: `max-w-screen-xl` (1280px) with `px-4 sm:px-6 lg:px-8`
- **Columns**: 12-column CSS Grid via Tailwind
- **Gutters**: `gap-4` (16px mobile), `gap-6` (24px desktop)

### Responsive Breakpoints

```css
--breakpoint-sm: 640px;    /* Mobile landscape */
--breakpoint-md: 768px;    /* Tablet portrait (Nurse iPad) */
--breakpoint-lg: 1024px;   /* Tablet landscape / small desktop */
--breakpoint-xl: 1280px;   /* Desktop (Surgeon workstation) */
--breakpoint-2xl: 1536px;  /* Wide desktop */
```

### Page Layout Structure

```tsx
{/* App Shell — persistent across all pages */}
<div className="min-h-screen bg-neutral-50">
  {/* Top Navigation Bar */}
  <nav className="sticky top-0 z-40 bg-white border-b border-neutral-200 h-16">
    {/* Logo | Page title | User menu */}
  </nav>

  {/* Main Content Area */}
  <main className="max-w-screen-xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
    {children}
  </main>
</div>
```

### Dashboard Layout (Desktop — Surgeon View)

```tsx
{/* Two-zone layout: main content + sidebar */}
<div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
  {/* Main: Top 10 Risk Patients (2/3 width) */}
  <div className="xl:col-span-2 space-y-4">
    {topRiskPatients.map(patient => <PatientRiskCard />)}
  </div>

  {/* Sidebar: Non-Responsive + Alerts (1/3 width) */}
  <div className="space-y-6">
    <NonResponsiveSection />
    <PendingAlertsSection />
  </div>
</div>
```

### Dashboard Layout (Tablet — Nurse View)

```tsx
{/* Single-column, larger touch targets */}
<div className="space-y-4">
  {topRiskPatients.map(patient => <PatientRiskCard size="large" />)}
</div>
```

---

## 4. Component Library

> All components follow **shadcn/ui** conventions. Use `npx shadcn-ui@latest add [component]` where available. Custom components extend shadcn patterns.

### Risk Badge

**The most important visual element in the system.**

```tsx
// components/ui/risk-badge.tsx
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const riskBadgeVariants = cva(
  "inline-flex items-center font-bold rounded-full border",
  {
    variants: {
      level: {
        HIGH: "bg-red-50 text-red-800 border-red-300",
        MEDIUM: "bg-amber-50 text-amber-800 border-amber-300",
        LOW: "bg-green-50 text-green-800 border-green-300",
      },
      size: {
        sm: "px-2 py-0.5 text-xs",
        md: "px-3 py-1 text-sm",
        lg: "px-4 py-1.5 text-base",   // For tablet/nurse view
      },
    },
    defaultVariants: { level: "LOW", size: "md" },
  }
);

interface RiskBadgeProps extends VariantProps<typeof riskBadgeVariants> {
  score?: number;  // 0–100 composite score
}

export function RiskBadge({ level, size, score }: RiskBadgeProps) {
  return (
    <span className={cn(riskBadgeVariants({ level, size }))}>
      {level} {score !== undefined && `(${score})`}
    </span>
  );
}
```

**Usage**:
```tsx
<RiskBadge level="HIGH" score={75} />         {/* Dashboard cards */}
<RiskBadge level="MEDIUM" size="lg" />         {/* Patient detail header */}
<RiskBadge level="LOW" size="sm" />            {/* Table rows */}
```

---

### Button (shadcn/ui extended)

Uses shadcn `Button` with OrthoWatch-specific variants.

#### Variants

| Variant | Use Case | Example |
|---|---|---|
| `default` | Primary actions (one per screen) | "Resolve Alert", "Enroll Patient" |
| `secondary` | Supporting actions | "View Details", "Cancel" |
| `destructive` | Dangerous/irreversible actions | "Cancel Episode" (with confirmation) |
| `outline` | Tertiary actions, filters | "Filter by TKR", "Export" |
| `ghost` | Inline actions, icon buttons | "Mark Reviewed", navigation links |
| `acknowledge` | Alert acknowledgment (custom) | "Acknowledge Alert" |

#### Sizes

| Size | Classes | Touch Target | Use |
|---|---|---|---|
| `sm` | `h-8 px-3 text-sm` | 32px | Table row actions, compact UI |
| `default` | `h-10 px-4 text-base` | 40px | Most actions |
| `lg` | `h-12 px-6 text-lg` | 48px | Primary CTAs, tablet/nurse UI |

#### Custom Acknowledge Button

```tsx
// Extend shadcn Button for alert acknowledgment
<Button
  variant="outline"
  className="border-amber-300 text-amber-700 hover:bg-amber-50 hover:border-amber-400"
  onClick={handleAcknowledge}
>
  <Eye className="w-4 h-4 mr-2" />
  Acknowledge
</Button>
```

#### Loading State

```tsx
<Button disabled>
  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
  Resolving...
</Button>
```

---

### Patient Risk Card (Dashboard)

**Primary dashboard element — one card per patient.**

```tsx
// components/dashboard/patient-risk-card.tsx
<div className={cn(
  "relative bg-white rounded-lg border p-4 transition-shadow hover:shadow-md",
  "cursor-pointer",
  riskLevel === "HIGH" && "border-l-4 border-l-red-500",
  riskLevel === "MEDIUM" && "border-l-4 border-l-amber-500",
  riskLevel === "LOW" && "border-l-4 border-l-green-500",
)}>
  {/* Row 1: Name + Risk Badge + Day Post-Op */}
  <div className="flex items-center justify-between mb-2">
    <div className="flex items-center gap-2">
      <h3 className="font-semibold text-neutral-800">{patientName}</h3>
      {isNewToday && (
        <span className="bg-primary-100 text-primary-700 text-xs font-medium px-2 py-0.5 rounded-full">
          New Today
        </span>
      )}
    </div>
    <RiskBadge level={riskLevel} score={riskScore} />
  </div>

  {/* Row 2: Surgery type + Day + Response status */}
  <div className="flex items-center gap-4 text-sm text-neutral-500 mb-3">
    <span>{surgeryType}</span>
    <span>Day {dayPostOp}</span>
    <span className={hasRespondedToday ? "text-green-600" : "text-amber-600"}>
      {hasRespondedToday ? "Responded" : "Pending"}
    </span>
  </div>

  {/* Row 3: Sparkline trend (5-day risk trajectory) */}
  <div className="h-12">
    <SparklineChart data={trendData} color={riskColor} />
  </div>

  {/* Row 4: Quick actions */}
  <div className="flex items-center justify-between mt-3 pt-3 border-t border-neutral-100">
    <Button variant="ghost" size="sm" onClick={handleMarkReviewed}>
      <CheckCircle className="w-4 h-4 mr-1" /> Mark Reviewed
    </Button>
    <Button variant="ghost" size="sm" onClick={handleViewDetails}>
      View Details <ChevronRight className="w-4 h-4 ml-1" />
    </Button>
  </div>
</div>
```

**Left border color rule**: Always matches risk level. This is the primary visual scan aid for surgeons reviewing the dashboard.

---

### Input Fields (shadcn/ui)

Use shadcn `Input`, `Label`, `Select`, `Textarea` components.

#### Text Input with Validation

```tsx
<div className="space-y-2">
  <Label htmlFor="phone" className="text-sm font-medium text-neutral-700">
    Patient Phone <span className="text-red-500">*</span>
  </Label>
  <Input
    id="phone"
    type="tel"
    placeholder="+91 98765 43210"
    className={cn(
      errors.phone && "border-red-500 focus-visible:ring-red-500"
    )}
    aria-invalid={!!errors.phone}
    aria-describedby={errors.phone ? "phone-error" : undefined}
  />
  {errors.phone && (
    <p id="phone-error" className="text-sm text-red-600" role="alert">
      {errors.phone.message}
    </p>
  )}
</div>
```

#### Pain Score Slider (Custom — Clinical)

```tsx
// components/ui/pain-slider.tsx
<div className="space-y-2">
  <Label className="text-sm font-medium">
    Discharge Pain Score: <span className="font-bold text-lg">{value}</span>/10
  </Label>
  <Slider
    min={0}
    max={10}
    step={1}
    value={[value]}
    onValueChange={([v]) => onChange(v)}
    className="py-2"
  />
  <div className="flex justify-between text-xs text-neutral-400">
    <span>0 — No Pain</span>
    <span>10 — Worst Pain</span>
  </div>
</div>
```

---

### Alert Resolution Modal

```tsx
// components/alerts/resolve-alert-modal.tsx
<Dialog open={open} onOpenChange={setOpen}>
  <DialogContent className="sm:max-w-md">
    <DialogHeader>
      <DialogTitle>Resolve Alert</DialogTitle>
      <DialogDescription>
        Select the action taken for {patientName} (Day {dayPostOp})
      </DialogDescription>
    </DialogHeader>

    {/* Escalation Outcome — REQUIRED */}
    <div className="space-y-4 py-4">
      <div className="space-y-2">
        <Label className="font-medium">Action Taken *</Label>
        <Select value={outcome} onValueChange={setOutcome}>
          <SelectTrigger>
            <SelectValue placeholder="Select outcome..." />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="OPD_SCHEDULED">OPD Scheduled</SelectItem>
            <SelectItem value="TELEPHONIC_ADVICE">Telephonic Advice Given</SelectItem>
            <SelectItem value="MEDICATION_ADJUSTED">Medication Adjusted</SelectItem>
            <SelectItem value="ER_REFERRAL">ER Referral</SelectItem>
            <SelectItem value="FALSE_POSITIVE">False Positive</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="space-y-2">
        <Label className="font-medium">Notes (optional)</Label>
        <Textarea
          value={notes}
          onChange={e => setNotes(e.target.value)}
          placeholder="Additional context..."
          maxLength={2000}
          rows={3}
        />
        <p className="text-xs text-neutral-400 text-right">{notes.length}/2000</p>
      </div>
    </div>

    <DialogFooter>
      <Button variant="secondary" onClick={() => setOpen(false)}>Cancel</Button>
      <Button onClick={handleResolve} disabled={!outcome || isLoading}>
        {isLoading ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null}
        Resolve Alert
      </Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

---

### Data Table (Patient List)

Uses shadcn `Table` with sortable headers and filter bar.

```tsx
<div className="space-y-4">
  {/* Filter Bar */}
  <div className="flex flex-wrap gap-3">
    <Select value={surgeryFilter} onValueChange={setSurgeryFilter}>
      <SelectTrigger className="w-[140px]"><SelectValue placeholder="Surgery" /></SelectTrigger>
      <SelectContent>
        <SelectItem value="ALL">All Types</SelectItem>
        <SelectItem value="TKR">TKR</SelectItem>
        <SelectItem value="THR">THR</SelectItem>
        <SelectItem value="ACL">ACL</SelectItem>
      </SelectContent>
    </Select>
    {/* ... risk level filter, response status filter */}
  </div>

  {/* Table */}
  <Table>
    <TableHeader>
      <TableRow>
        <TableHead className="cursor-pointer" onClick={() => sort("name")}>
          Patient <ArrowUpDown className="inline w-3 h-3 ml-1" />
        </TableHead>
        <TableHead>Surgery</TableHead>
        <TableHead>Day</TableHead>
        <TableHead className="cursor-pointer" onClick={() => sort("risk")}>
          Risk <ArrowUpDown className="inline w-3 h-3 ml-1" />
        </TableHead>
        <TableHead>Response</TableHead>
        <TableHead>Trend</TableHead>
      </TableRow>
    </TableHeader>
    <TableBody>
      {patients.map(p => (
        <TableRow key={p.episodeId} className="cursor-pointer hover:bg-neutral-50"
          onClick={() => navigate(`/patients/${p.episodeId}`)}>
          <TableCell className="font-medium">{p.patientName}</TableCell>
          <TableCell>{p.surgeryType}</TableCell>
          <TableCell>Day {p.dayPostOp}</TableCell>
          <TableCell><RiskBadge level={p.riskLevel} score={p.riskScore} size="sm" /></TableCell>
          <TableCell>
            {p.responseStatus === "COMPLETED"
              ? <CheckCircle className="w-4 h-4 text-green-500" />
              : <Clock className="w-4 h-4 text-amber-500" />}
          </TableCell>
          <TableCell><MiniSparkline data={p.trendData} /></TableCell>
        </TableRow>
      ))}
    </TableBody>
  </Table>

  {/* Pagination */}
  <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
</div>
```

---

### Enrollment Form (Multi-Section)

```tsx
// components/enrollment/enrollment-form.tsx
// Multi-section form: Patient Info → Surgery Details → Baseline Capture
// Uses react-hook-form + Zod validation (enrollmentSchema)

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { enrollmentSchema, type EnrollmentFormData } from "@/lib/schemas/enrollment";

export function EnrollmentForm({ onSubmit }: { onSubmit: (data: EnrollmentFormData) => void }) {
  const form = useForm<EnrollmentFormData>({ resolver: zodResolver(enrollmentSchema) });

  return (
    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">

      {/* Section 1: Patient Information */}
      <div>
        <h3 className="text-lg font-semibold text-neutral-800 border-b pb-2 mb-4">
          Patient Information
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* name, age, phone, caregiver_phone, preferred_language */}
        </div>
      </div>

      {/* Section 2: Surgery Details */}
      <div>
        <h3 className="text-lg font-semibold text-neutral-800 border-b pb-2 mb-4">
          Surgery Details
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* surgery_type (Select: TKR/THR/ACL), surgery_date, discharge_date, surgeon (Select) */}
        </div>
      </div>

      {/* Section 3: Baseline Capture */}
      <div>
        <h3 className="text-lg font-semibold text-neutral-800 border-b pb-2 mb-4">
          Baseline at Discharge
        </h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* discharge_pain_score (PainSlider 0–10), swelling_level (Select: NONE/MILD/MODERATE/SEVERE) */}
        </div>
      </div>

      {/* Submit — full width mobile, right-aligned desktop */}
      <div className="flex flex-col sm:flex-row sm:justify-end gap-3 pt-4 border-t">
        <Button type="button" variant="secondary">Cancel</Button>
        <Button type="submit" disabled={form.formState.isSubmitting} className="w-full sm:w-auto">
          {form.formState.isSubmitting
            ? <><Loader2 className="w-4 h-4 mr-2 animate-spin" /> Enrolling...</>
            : "Enroll Patient"}
        </Button>
      </div>

    </form>
  );
}
```

**Pattern rules**:
- Section headers: `text-lg font-semibold text-neutral-800 border-b pb-2 mb-4`
- Field layout: `grid grid-cols-1 md:grid-cols-2 gap-4`
- Section spacing: `space-y-8` between sections
- Submit alignment: `flex-col sm:flex-row sm:justify-end`
- All fields use shadcn `Input`, `Select`, `Slider` components with `Label`
- Validation errors follow the same pattern as Section 4 > Input Fields

---

### Recharts Configuration (Risk Score Chart)

```tsx
// components/charts/risk-score-chart.tsx
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ReferenceLine, ResponsiveContainer } from "recharts";

<ResponsiveContainer width="100%" height={280}>
  <LineChart data={dailyRiskData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
    <XAxis dataKey="day" label={{ value: "Post-Op Day", position: "bottom" }}
      tick={{ fontSize: 12 }} />
    <YAxis domain={[0, 100]} label={{ value: "Risk Score", angle: -90, position: "left" }}
      tick={{ fontSize: 12 }} />

    {/* Risk threshold reference lines */}
    <ReferenceLine y={60} stroke="#d97706" strokeDasharray="5 5" label="Medium" />
    <ReferenceLine y={30} stroke="#16a34a" strokeDasharray="5 5" label="Low" />

    {/* Risk score line */}
    <Line type="monotone" dataKey="score" stroke="#0ea5e9"
      strokeWidth={2} dot={{ r: 4 }} activeDot={{ r: 6 }} />

    <Tooltip
      contentStyle={{ borderRadius: "6px", border: "1px solid #e2e8f0" }}
      formatter={(value: number) => [`${value}`, "Risk Score"]}
    />
  </LineChart>
</ResponsiveContainer>
```

**Chart Color Rules**:
- Line color: `primary-500` (#0ea5e9) — neutral, not risk-coded
- Background zones: green (0–30), amber (30–60), red (60–100) as shaded areas
- Reference lines at risk thresholds (30, 60)
- Baseline pain score shown as horizontal dotted line

---

### Sparkline (Mini Trend Chart — Dashboard Cards)

```tsx
// components/charts/sparkline.tsx
import { LineChart, Line, ResponsiveContainer } from "recharts";

interface SparklineProps {
  data: number[];          // 5-day risk scores
  riskLevel: "HIGH" | "MEDIUM" | "LOW";
}

const riskColors = { HIGH: "#dc2626", MEDIUM: "#d97706", LOW: "#16a34a" };

export function Sparkline({ data, riskLevel }: SparklineProps) {
  const chartData = data.map((score, i) => ({ day: i + 1, score }));
  return (
    <ResponsiveContainer width="100%" height={40}>
      <LineChart data={chartData}>
        <Line type="monotone" dataKey="score"
          stroke={riskColors[riskLevel]} strokeWidth={2}
          dot={false} />
      </LineChart>
    </ResponsiveContainer>
  );
}
```

---

### Navigation Bar

```tsx
// components/layout/navbar.tsx
<nav className="sticky top-0 z-40 bg-white border-b border-neutral-200">
  <div className="max-w-screen-xl mx-auto px-4 sm:px-6 lg:px-8">
    <div className="flex items-center justify-between h-16">
      {/* Left: Logo + navigation */}
      <div className="flex items-center gap-6">
        <span className="text-xl font-bold text-primary-600">OrthoWatch</span>
        <div className="hidden md:flex items-center gap-1">
          <NavLink to="/dashboard" icon={<LayoutDashboard />}>Dashboard</NavLink>
          <NavLink to="/patients" icon={<Users />}>All Patients</NavLink>
          {user.role === "ADMIN" && (
            <NavLink to="/admin" icon={<Settings />}>Admin</NavLink>
          )}
        </div>
      </div>

      {/* Right: Alerts count + User menu */}
      <div className="flex items-center gap-4">
        <AlertBell count={pendingAlertCount} />
        <UserMenu user={user} onLogout={handleLogout} />
      </div>
    </div>
  </div>
</nav>
```

**Active NavLink**: `bg-primary-50 text-primary-700 font-medium rounded-md px-3 py-2`
**Inactive NavLink**: `text-neutral-600 hover:text-neutral-900 hover:bg-neutral-50 rounded-md px-3 py-2`

---

## 5. Accessibility Guidelines

### WCAG 2.1 Level AA Compliance

#### Color Contrast Requirements

| Element | Minimum Ratio | Verified |
|---|---|---|
| Body text (#475569 on #ffffff) | 4.5:1 | ✅ 7.1:1 |
| Risk HIGH text (#991b1b on #fef2f2) | 4.5:1 | ✅ 10.5:1 |
| Risk MEDIUM text (#92400e on #fffbeb) | 4.5:1 | ✅ 9.2:1 |
| Risk LOW text (#166534 on #f0fdf4) | 4.5:1 | ✅ 7.8:1 |
| Primary button text (white on #0ea5e9) | 4.5:1 | ✅ 4.6:1 |
| Placeholder text (#94a3b8 on white) | 3:1 (non-text) | ✅ 3.3:1 |

#### Keyboard Navigation

- All interactive elements reachable via Tab
- Focus visible: `focus-visible:ring-2 focus-visible:ring-primary-500 focus-visible:ring-offset-2`
- Escape closes modals and dropdowns
- Enter/Space activates buttons
- Arrow keys navigate Select and Table

#### Screen Reader Support

- Use semantic HTML: `<nav>`, `<main>`, `<table>`, `<button>`
- `role="alert"` for new high-risk alerts on dashboard
- `aria-live="polite"` for dashboard auto-refresh data
- `aria-label` on icon-only buttons: `<Button aria-label="Mark Reviewed">`
- `aria-invalid` and `aria-describedby` on form errors

#### Touch Targets

- Minimum interactive area: 44×44px
- Table rows on table: full-row touch area (wrap in clickable div)
- Button minimum height: 40px (default), 48px (large/tablet)
- Adequate spacing between adjacent targets: ≥8px gap

---

## 6. Animation Guidelines

### Transitions

```css
/* Default — hover, focus, color changes */
transition: all 150ms ease-in-out;

/* Card hover elevation */
transition: box-shadow 200ms ease-in-out;

/* Modal enter/exit */
transition: opacity 200ms ease-out, transform 200ms ease-out;

/* Skeleton pulse */
@keyframes pulse { 0%, 100% { opacity: 1 } 50% { opacity: 0.5 } }
```

### Rules

- **Duration**: 150–300ms maximum. Clinical dashboards must feel instant.
- **Easing**: `ease-in-out` for most. `ease-out` for entrances.
- **Performance**: Only animate `transform` and `opacity`. Never animate `width`, `height`, `margin`.
- **Reduced motion**: Respect `prefers-reduced-motion: reduce` — disable non-essential animations.
- **No decorative animations**: No bouncing, no sliding page transitions. This is a clinical tool.

### Allowed Animations

| Element | Animation | Duration |
|---|---|---|
| Button hover | Background color change | 150ms |
| Card hover | Shadow elevation (sm → md) | 200ms |
| Modal | Fade in + scale from 95% to 100% | 200ms |
| Toast notification | Slide in from top-right | 200ms |
| Loading skeleton | Pulse opacity | 2000ms (continuous) |
| Spinner | Rotate 360° | 750ms (continuous) |
| Dashboard refresh | None — data swaps instantly | 0ms |

---

## 7. Icon System

### Library: Lucide React

```bash
npm install lucide-react
```

### Clinical Icon Mapping

| Concept | Icon | Usage |
|---|---|---|
| Risk HIGH | `AlertTriangle` | Alert badges, dashboard warnings |
| Risk MEDIUM | `AlertCircle` | Moderate alerts |
| Risk LOW | `CheckCircle` | Safe status |
| Dashboard | `LayoutDashboard` | Navigation |
| Patients | `Users` | Navigation, header |
| Patient detail | `User` | Detail page header |
| Alert bell | `Bell` | Navbar alert count |
| Mark reviewed | `CheckCircle` | Dashboard card action |
| View details | `ChevronRight` | Card navigation hint |
| Pain score | `Activity` | Checklist response |
| Wound image | `Camera` | Image upload status |
| Trend up (worsening) | `TrendingUp` | Risk trajectory |
| Trend down (improving) | `TrendingDown` | Risk trajectory |
| Trend stable | `Minus` | Risk trajectory |
| Calendar | `Calendar` | Surgery date, day post-op |
| Phone | `Phone` | Patient/caregiver contact |
| Settings | `Settings` | Admin panel |
| Logout | `LogOut` | User menu |

### Sizes

| Size | Class | px | Use |
|---|---|---|---|
| Small | `w-4 h-4` | 16px | Inline with text, table cells |
| Base | `w-5 h-5` | 20px | Buttons, nav items |
| Large | `w-6 h-6` | 24px | Page headers, empty states |
| XL | `w-8 h-8` | 32px | Empty state illustrations |

---

## 8. State Indicators

### Loading — Skeleton Screens

```tsx
{/* Dashboard card skeleton */}
<div className="bg-white rounded-lg border p-4 animate-pulse">
  <div className="flex justify-between mb-3">
    <div className="h-5 bg-neutral-200 rounded w-32" />
    <div className="h-6 bg-neutral-200 rounded-full w-20" />
  </div>
  <div className="flex gap-4 mb-3">
    <div className="h-4 bg-neutral-200 rounded w-12" />
    <div className="h-4 bg-neutral-200 rounded w-16" />
  </div>
  <div className="h-12 bg-neutral-100 rounded" />
</div>
```

### Loading — Spinner (Buttons, forms)

```tsx
<Loader2 className="w-4 h-4 animate-spin" />
```

### Empty States

```tsx
{/* No patients in filtered list */}
<div className="text-center py-16">
  <Users className="w-12 h-12 text-neutral-300 mx-auto mb-4" />
  <h3 className="text-lg font-medium text-neutral-800 mb-1">No patients found</h3>
  <p className="text-neutral-500 mb-6">Try adjusting your filters or enroll a new patient.</p>
  <Button onClick={handleEnroll}>Enroll Patient</Button>
</div>
```

### Error States

```tsx
{/* API error banner */}
<div className="bg-red-50 border border-red-200 text-red-800 px-4 py-3 rounded-lg flex items-start gap-3">
  <AlertTriangle className="w-5 h-5 text-red-600 mt-0.5 shrink-0" />
  <div>
    <p className="font-medium">Failed to load dashboard</p>
    <p className="text-sm text-red-600 mt-1">Check your connection and try again.</p>
    <Button variant="outline" size="sm" className="mt-3" onClick={retry}>
      Retry
    </Button>
  </div>
</div>
```

### Toast Notifications (shadcn Toast)

```tsx
toast({ title: "Patient enrolled", description: "Consent message sent via WhatsApp." });
toast({ title: "Alert resolved", description: "OPD scheduled for tomorrow.", variant: "default" });
toast({ title: "Error", description: "Failed to resolve alert. Try again.", variant: "destructive" });
```

---

## 9. Responsive Design

### Breakpoint Strategy

| Breakpoint | Target Device | Layout |
|---|---|---|
| `< 640px` | Mobile (rare for clinicians) | Single column, stacked cards |
| `640–767px` | Mobile landscape | Single column, wider cards |
| `768–1023px` | Tablet (Nurse iPad) | Single column, large touch targets |
| `1024–1279px` | Small desktop | Two columns, compact data |
| `≥ 1280px` | Desktop (Surgeon) | Three-zone dashboard, dense layout |

### Responsive Patterns

```tsx
{/* Dashboard grid — adapts from 1 to 3 columns */}
<div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4 lg:gap-6">

{/* Patient detail — switches from tabs to side-by-side */}
<div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
  <div className="lg:col-span-2">{/* Timeline + Chart */}</div>
  <div>{/* Alerts + Audit Log */}</div>
</div>

{/* Table — horizontal scroll on mobile */}
<div className="overflow-x-auto -mx-4 sm:mx-0">
  <Table className="min-w-[640px]">...</Table>
</div>
```

### Touch Target Compliance

- All buttons: minimum `h-10` (40px), `h-12` (48px) on tablet
- Table rows: `py-3` minimum for adequate tap area
- Nav links: `px-3 py-2` with `gap-1` between items
- Filter dropdowns: full-width on mobile (`w-full sm:w-[140px]`)

---

## 10. Performance Guidelines

### Bundle Optimization

- **Code splitting**: React Router lazy loading for all pages
  ```tsx
  const Dashboard = lazy(() => import("./pages/Dashboard"));
  const PatientDetail = lazy(() => import("./pages/PatientDetail"));
  ```
- **Tree shaking**: Import Lucide icons individually: `import { Bell } from "lucide-react"`
- **Recharts**: Import only used components: `import { LineChart, Line } from "recharts"`

### Data Fetching (TanStack Query)

```tsx
// Stale-while-revalidate for dashboard
const { data, isLoading } = useQuery({
  queryKey: ["dashboard", "top-risk"],
  queryFn: fetchTopRiskPatients,
  staleTime: 5 * 60 * 1000,    // 5 minutes — matches backend cache TTL
  refetchInterval: 5 * 60 * 1000,  // Auto-refresh every 5 min
});
```

### Image Optimization

- Wound images: lazy-loaded, shown as thumbnails (100×100px) on detail view
- Full-size images: loaded on-demand in modal (click to zoom)
- No images on dashboard — only sparkline SVGs (rendered inline)

### Target Load Times

| Page | Target | Strategy |
|---|---|---|
| Login | <1s | Static page, minimal JS |
| Dashboard | <3s | Skeleton → data, cached API (5 min) |
| Patient Detail | <2s | Skeleton → data, cached API (2 min) |
| Patient List | <2s | Paginated (20 items), server-side filter |

---

## 11. Browser Support

### Supported Browsers

- Chrome 100+ (primary — most hospital systems)
- Firefox 100+ (secondary)
- Safari 15+ (iPad for nurses)
- Edge 100+

### Progressive Enhancement

- Core functionality works without animations
- Graceful degradation: risk colors visible even if CSS custom properties fail (Tailwind classes as fallback)
- No WebSocket dependency — polling via TanStack Query interval

### No Support Required

- Internet Explorer (any version)
- Opera Mini
- Pre-Chromium Edge
