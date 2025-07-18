<script>
  import { notifications, removeNotification } from '../lib/stores.js';

  function getNotificationClass(type) {
    switch (type) {
      case 'success':
        return 'success';
      case 'error':
        return 'error';
      case 'warning':
        return 'warning';
      default:
        return 'info';
    }
  }

  function getNotificationIcon(type) {
    switch (type) {
      case 'success':
        return '✅';
      case 'error':
        return '❌';
      case 'warning':
        return '⚠️';
      default:
        return 'ℹ️';
    }
  }
</script>

<div class="notifications-container">
  {#each $notifications as notification (notification.id)}
    <div class="notification {getNotificationClass(notification.type)}">
      <div class="notification-content">
        <span class="notification-icon">{getNotificationIcon(notification.type)}</span>
        <span class="notification-message">{notification.message}</span>
      </div>
      <button 
        class="notification-close"
        on:click={() => removeNotification(notification.id)}
      >
        ×
      </button>
    </div>
  {/each}
</div>

<style>
  .notifications-container {
    position: fixed;
    top: 1rem;
    right: 1rem;
    z-index: 1000;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    pointer-events: none;
  }

  .notification {
    min-width: 300px;
    max-width: 400px;
    padding: 1rem;
    border-radius: 0.5rem;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 1rem;
    pointer-events: all;
    animation: slideIn 0.3s ease-out;
  }

  @keyframes slideIn {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }

  .notification.info {
    background-color: #dbeafe;
    border-left: 4px solid #3b82f6;
    color: #1e40af;
  }

  .notification.success {
    background-color: #d1fae5;
    border-left: 4px solid #10b981;
    color: #065f46;
  }

  .notification.warning {
    background-color: #fef3c7;
    border-left: 4px solid #f59e0b;
    color: #92400e;
  }

  .notification.error {
    background-color: #fee2e2;
    border-left: 4px solid #ef4444;
    color: #991b1b;
  }

  .notification-content {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    flex: 1;
  }

  .notification-icon {
    font-size: 1.25rem;
    flex-shrink: 0;
  }

  .notification-message {
    font-size: 0.875rem;
    font-weight: 500;
    line-height: 1.4;
  }

  .notification-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: inherit;
    opacity: 0.7;
    transition: opacity 0.2s;
    padding: 0;
    width: 1.5rem;
    height: 1.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 0.25rem;
  }

  .notification-close:hover {
    opacity: 1;
    background-color: rgba(0, 0, 0, 0.1);
  }
</style>