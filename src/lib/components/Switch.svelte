<!--
  Switch.svelte
  オン/オフのトグルスイッチ。
-->
<script lang="ts">
	interface Props {
		checked: boolean;
		onchange?: (checked: boolean) => void;
		label?: string;
		disabled?: boolean;
		id?: string;
	}

	let { checked = $bindable(false), onchange, label, disabled = false, id }: Props = $props();

	function toggle() {
		if (disabled) return;
		checked = !checked;
		onchange?.(checked);
	}
</script>

<label
	class="inline-flex items-center gap-3"
	class:opacity-50={disabled}
	class:cursor-not-allowed={disabled}
>
	<button
		type="button"
		role="switch"
		aria-checked={checked}
		aria-label={label}
		{id}
		{disabled}
		onclick={toggle}
		class="relative inline-flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-full transition-colors focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-primary-500)] focus-visible:ring-offset-2"
		class:bg-[var(--color-primary-500)]={checked}
		class:bg-[var(--color-surface-border)]={!checked}
	>
		<span
			class="inline-block h-5 w-5 transform rounded-full bg-white shadow-md transition-transform"
			class:translate-x-5={checked}
			class:translate-x-0.5={!checked}
		></span>
	</button>
	{#if label}
		<span class="text-sm text-[var(--color-nav-active)]">{label}</span>
	{/if}
</label>
