export default function ModalShell({ title, subtitle, onClose, maxWidth = 'max-w-2xl', children, footer = null }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4" onClick={onClose}>
      <div
        className={`glass ${maxWidth} w-full rounded-2xl p-6 relative max-h-[90vh] overflow-y-auto`}
        onClick={(e) => e.stopPropagation()}
      >
        <button
          type="button"
          onClick={onClose}
          className="absolute top-4 right-4 text-zinc-400 hover:text-white"
          aria-label="Close"
        >
          ✕
        </button>
        <div className="mb-4 pr-8">
          <h3 className="text-xl font-bold">{title}</h3>
          {subtitle ? <p className="text-sm text-zinc-500 mt-1">{subtitle}</p> : null}
        </div>
        <div>{children}</div>
        {footer ? <div className="mt-5 flex items-center justify-end gap-3">{footer}</div> : null}
      </div>
    </div>
  );
}

