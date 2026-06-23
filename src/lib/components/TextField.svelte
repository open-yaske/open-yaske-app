<!--
  TextField.svelte
  ラベル + 入力 + 補助テキスト。
  input, textarea, select の3種類に対応。
-->
<script lang="ts">
	import type { Component } from 'svelte';

	type FieldType = 'text' | 'email' | 'password' | 'number' | 'url' | 'tel' | 'date' | 'time';

	interface Option {
		value: string;
		label: string;
	}

	interface Props {
		value: string;
		type?: FieldType | 'textarea' | 'select';
		label?: string;
		placeholder?: string;
		hint?: string;
		error?: string;
		required?: boolean;
		disabled?: boolean;
		readonly?: boolean;
		autocomplete?: string;
		leadingIcon?: Component<{ size?: number }>;
		trailingIcon?: Component<{ size?: number }>;
		options?: Option[];
		rows?: number;
		id?: string;
		name?: string;
		oninput?: (e: Event) => void;
		onchange?: (e: Event) => void;
		onblur?: (e: FocusEvent) => void;
	}

	let {
		value = $bindable(''),
		type = 'text',
		label,
		placeholder,
		hint,
		error,
		required = false,
		disabled = false,
		readonly = false,
		autocomplete,
		leadingIcon: Leading,
		trailingIcon: Trailing,
		options = [],
		rows = 3,
		id,
		name,
		oninput,
		onchange,
		onblur
	}: Props = $props();

	const fieldId = $derived(id ?? `tf-${Math.random().toString(36).slice(2, 9)}`);
	const hasError = $derived(!!error);
</script>

<div class="w-full">
	{#if label}
		<label for={fieldId} class="mb-1 block text-sm font-medium text-[var(--color-nav-active)]">
			{label}
			{#if required}<span class="text-red-500">*</span>{/if}
		</label>
	{/if}
	<div class="relative">
		{#if Leading}
			<span
				class="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-[var(--color-nav-inactive)]"
			>
				<Leading size={16} />
			</span>
		{/if}

		{#if type === 'textarea'}
			<textarea
				id={fieldId}
				{name}
				bind:value
				{placeholder}
				{required}
				{disabled}
				{readonly}
				{rows}
				{oninput}
				{onchange}
				{onblur}
				class="block w-full rounded-lg border bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30 disabled:opacity-50 {Leading
					? 'pl-10'
					: ''} {Trailing ? 'pr-10' : ''} {hasError
					? 'border-red-500'
					: 'border-[var(--color-surface-border)]'}"></textarea>
		{:else if type === 'select'}
			<div class="relative w-full">
				<select
					id={fieldId}
					{name}
					bind:value
					{required}
					{disabled}
					{onchange}
					class="block w-full appearance-none rounded-lg border bg-[var(--color-surface-card)] px-3.5 py-2.5 text-sm text-[var(--color-nav-active)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30 disabled:opacity-50 {Leading
						? 'pl-10'
						: ''} pr-10 {hasError
						? 'border-red-500'
						: 'border-[var(--color-surface-border)]'} cursor-pointer transition-colors hover:bg-[var(--color-surface-muted)]/20"
					style="-webkit-appearance: none; -moz-appearance: none; appearance: none;"
				>
					{#each options as opt (opt.value)}
						<option value={opt.value}>{opt.label}</option>
					{/each}
				</select>
				{#if !Trailing}
					<span
						class="pointer-events-none absolute top-1/2 right-3.5 -translate-y-1/2 text-[8px] text-[var(--color-nav-inactive)]"
					>
						▼
					</span>
				{/if}
			</div>
		{:else}
			<input
				id={fieldId}
				{name}
				{type}
				bind:value
				{placeholder}
				{required}
				{disabled}
				{readonly}
				autocomplete={autocomplete as HTMLInputElement['autocomplete']}
				{oninput}
				{onchange}
				{onblur}
				class="block w-full rounded-lg border bg-[var(--color-surface-card)] px-3 py-2 text-sm text-[var(--color-nav-active)] placeholder-[var(--color-nav-inactive)] focus:border-[var(--color-primary-500)] focus:outline-none focus:ring-2 focus:ring-[var(--color-primary-500)]/30 disabled:opacity-50 {Leading
					? 'pl-10'
					: ''} {Trailing ? 'pr-10' : ''} {hasError
					? 'border-red-500'
					: 'border-[var(--color-surface-border)]'}"
			/>
		{/if}

		{#if Trailing}
			<span
				class="pointer-events-none absolute top-1/2 right-3 -translate-y-1/2 text-[var(--color-nav-inactive)]"
			>
				<Trailing size={16} />
			</span>
		{/if}
	</div>
	{#if error}
		<p class="mt-1 text-xs text-red-500">{error}</p>
	{:else if hint}
		<p class="mt-1 text-xs text-[var(--color-nav-inactive)]">{hint}</p>
	{/if}
</div>
