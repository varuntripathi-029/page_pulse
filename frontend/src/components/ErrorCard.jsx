function ErrorCard({ message }) {
  return (
    <div className="bg-gray-100 border border-gray-300 rounded-lg p-4 mt-6" role="alert">
      <p className="m-0 text-black font-medium">{message}</p>
    </div>
  )
}

export default ErrorCard
